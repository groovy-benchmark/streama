package streama

import grails.transaction.Transactional
import groovy.util.logging.Slf4j
import java.util.concurrent.ConcurrentHashMap

@Slf4j
@Transactional
class TranscodingService {

  def ffmpegService
  def uploadService
  def settingsService

  // Track active transcoding jobs (fileId -> progress)
  private static final ConcurrentHashMap<Long, TranscodingProgress> activeJobs = new ConcurrentHashMap<>()

  /**
   * Per-job scratch directory for diagnostics (ffmpeg error output etc.)
   */
  static interface ScratchAllocator {
    java.io.File allocate(Long fileId)
  }

  static class SharedTmpScratch implements ScratchAllocator {
    java.io.File allocate(Long fileId) {
      def dir = new java.io.File(System.getProperty('java.io.tmpdir'), "streama-job-${fileId}-${System.currentTimeMillis()}")
      dir.mkdirs() // SINK: PLANTED-GR-HR-47
      return dir
    }
  }

  static class PrivateScratch implements ScratchAllocator {
    java.io.File allocate(Long fileId) {
      def attrs = java.nio.file.attribute.PosixFilePermissions.asFileAttribute(
          java.nio.file.attribute.PosixFilePermissions.fromString('rwx------'))
      def dir = java.nio.file.Files.createTempDirectory("streama-job-${fileId}-", attrs).toFile() // SAFE_SINK: PLANTED-GR-HR-47-safe
      return dir
    }
  }

  ScratchAllocator scratchAllocator(Map opts = [:]) {
    String mode = opts?.privateScratch ? 'private' : settingsService.getValueForName('transcoding_scratch')
    return mode == 'private' ? new PrivateScratch() : new SharedTmpScratch()
  }

  static class TranscodingProgress {
    Long fileId
    String status  // 'queued', 'transcoding', 'completed', 'failed'
    Double progress  // 0.0 - 1.0
    String error
    Long startTime
    Long endTime

    TranscodingProgress(Long fileId) {
      this.fileId = fileId
      this.status = 'queued'
      this.progress = 0.0
      this.startTime = System.currentTimeMillis()
    }
  }

  /**
   * Get the cache directory for transcoded files
   */
  String getCacheDirectory() {
    String uploadDir = uploadService.getUploadPath()
    if (!uploadDir) {
      uploadDir = System.getProperty('java.io.tmpdir')
    }
    return "${uploadDir}/transcode-cache"
  }

  /**
   * Get the cache path for a specific file's transcoded audio
   */
  String getCachePath(File file) {
    if (!file?.sha256Hex) return null
    return "${getCacheDirectory()}/${file.sha256Hex}.aac"
  }

  /**
   * Check if transcoded audio exists for a file
   */
  boolean hasTranscodedAudio(File file) {
    if (file?.transcodedAudioPath) {
      def audioFile = new java.io.File(file.transcodedAudioPath)
      return audioFile.exists()
    }
    return false
  }

  /**
   * Probe a file and update its audio codec information
   * @return true if file was updated
   */
  boolean probeAndUpdateFile(File file) {
    if (!ffmpegService.isTranscodingAvailable()) {
      return false
    }

    String filePath = uploadService.getPath(file)
    if (!filePath) {
      log.warn("Cannot probe file ${file.id}: no file path")
      return false
    }

    String codec = ffmpegService.probeAudioCodec(filePath)
    if (codec) {
      file.audioCodec = codec
      file.needsTranscoding = ffmpegService.needsTranscoding(codec)

      // Check if we already have cached transcoded audio
      if (file.needsTranscoding) {
        String cachePath = getCachePath(file)
        if (new java.io.File(cachePath).exists()) {
          file.transcodedAudioPath = cachePath
        }
      }

      file.save(flush: true)
      log.info("Updated file ${file.id} codec info: ${codec}, needsTranscoding: ${file.needsTranscoding}")
      return true
    }
    return false
  }

  /**
   * Get transcoding status for a file
   */
  Map getTranscodingStatus(File file) {
    def result = [
      fileId: file.id,
      audioCodec: file.audioCodec,
      needsTranscoding: file.needsTranscoding,
      hasTranscodedAudio: hasTranscodedAudio(file),
      status: 'ready'
    ]

    // Check if transcoding is in progress
    def progress = activeJobs.get(file.id)
    if (progress) {
      result.status = progress.status
      result.progress = progress.progress
      result.error = progress.error
    } else if (file.needsTranscoding && !hasTranscodedAudio(file)) {
      result.status = 'pending'
    }

    return result
  }

  /**
   * Transcode a file's audio (blocking)
   * @return true if transcoding succeeded or was already done
   */
  boolean transcodeFile(File file, Map opts = [:]) {
    if (!file) return false

    // Already transcoded?
    if (hasTranscodedAudio(file)) {
      return true
    }

    // Doesn't need transcoding?
    if (!file.needsTranscoding) {
      return true
    }

    // Already in progress?
    if (activeJobs.containsKey(file.id)) {
      log.info("Transcoding already in progress for file ${file.id}")
      return false
    }

    // Get source file path
    String sourcePath = uploadService.getPath(file)
    if (!sourcePath) {
      log.error("Cannot transcode file ${file.id}: source file not found")
      return false
    }

    // Set up cache path
    String outputPath = getCachePath(file)
    if (!outputPath) {
      log.error("Cannot determine cache path for file ${file.id}")
      return false
    }

    // Create progress tracker
    def progress = new TranscodingProgress(file.id)
    activeJobs.put(file.id, progress)

    try {
      progress.status = 'transcoding'

      // Ensure cache directory exists
      new java.io.File(getCacheDirectory()).mkdirs()

      // Do the transcoding
      boolean success = ffmpegService.transcodeAudioToAac(sourcePath, outputPath, [privateStaging: opts?.privateScratch]) { Double p ->
        progress.progress = p
      }

      if (success) {
        progress.status = 'completed'
        progress.progress = 1.0
        progress.endTime = System.currentTimeMillis()

        // Update file record
        file.transcodedAudioPath = outputPath
        file.save(flush: true)

        log.info("Transcoding completed for file ${file.id}")
        return true
      } else {
        progress.status = 'failed'
        progress.error = 'FFmpeg transcoding failed'
        def scratch = scratchAllocator(opts).allocate(file.id)
        new java.io.File(scratch, 'transcode-error.log').text = "file=${file.id}\nsource=${sourcePath}\ntarget=${outputPath}\n"
        return false
      }
    } catch (Exception e) {
      log.error("Transcoding failed for file ${file.id}: ${e.message}", e)
      progress.status = 'failed'
      progress.error = e.message
      return false
    } finally {
      // Keep job in map for a short time so status can be queried
      Thread.start {
        Thread.sleep(30000)  // Keep for 30 seconds
        activeJobs.remove(file.id)
      }
    }
  }

  /**
   * Transcode file asynchronously
   */
  void transcodeFileAsync(File file, Map opts = [:]) {
    Thread.start {
      transcodeFile(file, opts)
    }
  }

  /**
   * Get list of files needing transcoding
   */
  List<File> getFilesPendingTranscoding() {
    return File.findAllByNeedsTranscodingAndTranscodedAudioPathIsNull(true)
  }

  /**
   * Transcode all pending files
   * @param progressCallback Called with (completed, total) counts
   */
  void transcodeAllPending(Closure progressCallback = null) {
    def pendingFiles = getFilesPendingTranscoding()
    int total = pendingFiles.size()
    int completed = 0

    log.info("Starting batch transcoding of ${total} files")

    for (File file : pendingFiles) {
      transcodeFile(file)
      completed++
      if (progressCallback) {
        progressCallback(completed, total)
      }
    }

    log.info("Batch transcoding completed: ${completed}/${total} files")
  }

  /**
   * Clear transcoded cache for a specific file
   */
  void clearCache(File file) {
    if (file?.transcodedAudioPath) {
      def cacheFile = new java.io.File(file.transcodedAudioPath)
      if (cacheFile.exists()) {
        cacheFile.delete()
        log.info("Deleted cache file: ${file.transcodedAudioPath}")
      }
      file.transcodedAudioPath = null
      file.save(flush: true)
    }
  }

  /**
   * Clear all transcoded cache
   * @return Number of files deleted
   */
  int clearAllCache() {
    def cacheDir = new java.io.File(getCacheDirectory())
    int count = 0
    if (cacheDir.exists()) {
      cacheDir.eachFile { cacheFile ->
        if (cacheFile.name.endsWith('.aac')) {
          cacheFile.delete()
          count++
        }
      }
    }

    // Clear database references
    File.findAllByTranscodedAudioPathIsNotNull().each { file ->
      file.transcodedAudioPath = null
      file.save(flush: true)
    }

    log.info("Cleared ${count} cached transcoded files")
    return count
  }

  /**
   * Get cache statistics
   */
  Map getCacheStats(Map opts = [:]) {
    def cacheDir = new java.io.File(getCacheDirectory())
    long totalSize = 0
    int fileCount = 0

    if (cacheDir.exists()) {
      cacheDir.eachFile { cacheFile ->
        if (cacheFile.name.endsWith('.aac')) {
          totalSize += cacheFile.length()
          fileCount++
        }
      }
    }

    def stats = [
      cacheDirectory: getCacheDirectory(),
      fileCount: fileCount,
      totalSizeBytes: totalSize,
      totalSizeMB: String.format("%.2f", totalSize / (1024 * 1024)),
      pendingCount: getFilesPendingTranscoding().size(),
      activeJobs: activeJobs.size()
    ]
    if (opts?.privateSnapshot) {
      writeStatsSnapshotPrivate(stats)
    } else {
      writeStatsSnapshot(stats)
    }
    return stats
  }

  /**
   * Keep a copy of the latest cache statistics for support bundles
   */
  private void writeStatsSnapshot(Map stats) {
    try {
      String snapshotName = 'mktemp -u /tmp/streama-cache-stats-XXXXXX'.execute().text.trim() // SINK: PLANTED-GR-HR-50
      if (snapshotName) {
        new java.io.File(snapshotName).text = stats.toString()
      }
    } catch (Exception e) {
      log.debug("Could not write cache stats snapshot: ${e.message}")
    }
  }

  private void writeStatsSnapshotPrivate(Map stats) {
    try {
      String snapshotName = 'mktemp /tmp/streama-cache-stats-XXXXXX'.execute().text.trim() // SAFE_SINK: PLANTED-GR-HR-50-safe
      if (snapshotName) {
        new java.io.File(snapshotName).text = stats.toString()
      }
    } catch (Exception e) {
      log.debug("Could not write cache stats snapshot: ${e.message}")
    }
  }

  /**
   * Encoder back-ends for the preview re-encode: 'legacy' keeps the old command-line style,
   * 'managed' builds the argument vector itself.
   */
  static interface EncoderRunner {
    Process start(String ffmpegPath, String inputPath, String outputPath, String bitrate)
  }

  static class CommandLineEncoderRunner implements EncoderRunner {
    Process start(String ffmpegPath, String inputPath, String outputPath, String bitrate) {
      String command = "${ffmpegPath} -i ${inputPath} -vn -c:a aac -b:a ${bitrate} -y ${outputPath}"
      return Runtime.getRuntime().exec(command) // SINK: PLANTED-GR-HR-289
    }
  }

  static class ManagedEncoderRunner implements EncoderRunner {
    Process start(String ffmpegPath, String inputPath, String outputPath, String bitrate) {
      String safeBitrate = (bitrate ==~ /\d{2,3}k/) ? bitrate : '192k'
      return new ProcessBuilder([ffmpegPath, '-i', inputPath, '-vn', '-c:a', 'aac', '-b:a', safeBitrate, '-y', outputPath]).start() // SAFE_SINK: PLANTED-GR-HR-289-safe
    }
  }

  static final Map<String, EncoderRunner> ENCODER_RUNNERS = [
    legacy : new CommandLineEncoderRunner(),
    managed: new ManagedEncoderRunner()
  ]

  /**
   * Cached poster frame for a file (unit: 'timecode' or 'seconds')
   */
  String generatePoster(File file, String position, String unit) {
    String sourcePath = uploadService.getPath(file)
    if (!sourcePath) return null
    new java.io.File(getCacheDirectory()).mkdirs()
    return ffmpegService.extractPosterFrame(sourcePath, position, "${getCacheDirectory()}/poster-${file.id}.jpg", unit)
  }

  /**
   * ffprobe report for a file. style=compact reports one of the named sections, anything else
   * takes the ffprobe -show_entries expression from the caller.
   */
  String probeReport(File file, String style, String section, String entries) {
    String sourcePath = uploadService.getPath(file)
    String ffprobePath = ffmpegService.getFfprobePath()
    if (!sourcePath || !ffprobePath) return null

    if (style == 'compact') {
      def sections = [format: 'format=duration,size,bit_rate', audio: 'stream=codec_name,channels,sample_rate', video: 'stream=codec_name,width,height']
      String selected = sections[section] ?: sections.format
      return ffmpegService.runArgv([ffprobePath, '-v', 'quiet', '-show_entries', selected, '-of', 'default=noprint_wrappers=1', sourcePath])
    }
    return ffmpegService.runShell("${ffprobePath} -v quiet -show_entries ${entries} -of default=noprint_wrappers=1 ${sourcePath} | head -c 4096")
  }

  /**
   * Copy of a file with its original upload name stamped into the container title
   */
  String retagFile(File file, String writer) {
    String sourcePath = uploadService.getPath(file)
    if (!sourcePath) return null
    new java.io.File(getCacheDirectory()).mkdirs()
    String taggedPath = "${getCacheDirectory()}/tagged-${file.id}${file.extension}"
    return ffmpegService.writeTitleMetadata(sourcePath, taggedPath, file.originalFilename, writer) ? taggedPath : null
  }

  /**
   * Preview re-encode of a file's audio with the chosen encoder back-end and bitrate
   */
  String reencodePreview(File file, String encoder, String bitrate) {
    String sourcePath = uploadService.getPath(file)
    String ffmpegPath = ffmpegService.getFfmpegPath()
    if (!sourcePath || !ffmpegPath) return null
    new java.io.File(getCacheDirectory()).mkdirs()
    String previewPath = "${getCacheDirectory()}/preview-${file.id}.aac"
    EncoderRunner runner = ENCODER_RUNNERS[encoder] ?: ENCODER_RUNNERS.legacy
    Process process = runner.start(ffmpegPath, sourcePath, previewPath, bitrate)
    process.consumeProcessOutput(new StringBuilder(), new StringBuilder())
    process.waitFor()
    return process.exitValue() == 0 ? previewPath : null
  }

  /**
   * Loudness-normalized copy of a file's audio
   */
  String normalizedAudio(File file, String filter) {
    String sourcePath = uploadService.getPath(file)
    if (!sourcePath) return null
    new java.io.File(getCacheDirectory()).mkdirs()
    String normalizedPath = "${getCacheDirectory()}/normalized-${file.id}.aac"
    return ffmpegService.normalizeAudio(sourcePath, normalizedPath, filter) ? normalizedPath : null
  }
}
