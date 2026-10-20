package streama

import grails.transaction.Transactional
import groovy.util.logging.Slf4j

@Slf4j
@Transactional
class FfmpegService {

  def settingsService

  // Codecs that browsers cannot play natively
  static final List<String> INCOMPATIBLE_CODECS = [
    'eac3',      // Dolby Digital Plus
    'ac3',       // Dolby Digital
    'dts',       // DTS
    'truehd',    // Dolby TrueHD
    'pcm_s16le', // PCM audio
    'pcm_s24le',
    'pcm_s32le',
    'flac'       // FLAC (limited browser support)
  ]

  // Codecs that browsers can play
  static final List<String> COMPATIBLE_CODECS = [
    'aac',
    'mp3',
    'opus',
    'vorbis'
  ]

  /**
   * Detects if FFmpeg is available and returns its path
   * @return Map with 'found', 'ffmpegPath', 'ffprobePath', 'version'
   */
  Map detectFfmpeg() {
    def result = [found: false, ffmpegPath: null, ffprobePath: null, version: null]

    // First check if custom paths are configured
    String configuredFfmpeg = settingsService.getValueForName('ffmpeg_path')
    String configuredFfprobe = settingsService.getValueForName('ffprobe_path')

    // Try configured paths first, then auto-detect
    String ffmpegPath = configuredFfmpeg ?: findExecutable('ffmpeg')
    String ffprobePath = configuredFfprobe ?: findExecutable('ffprobe')

    if (ffmpegPath && ffprobePath) {
      // Verify they actually work
      try {
        def versionProcess = [ffmpegPath, '-version'].execute()
        versionProcess.waitFor()
        if (versionProcess.exitValue() == 0) {
          def versionOutput = versionProcess.text
          def versionMatch = versionOutput =~ /ffmpeg version (\S+)/
          result.version = versionMatch ? versionMatch[0][1] : 'unknown'
          result.found = true
          result.ffmpegPath = ffmpegPath
          result.ffprobePath = ffprobePath
        }
      } catch (Exception e) {
        log.warn("FFmpeg detection failed: ${e.message}")
      }
    }

    return result
  }

  /**
   * Find an executable in the system PATH
   */
  private String findExecutable(String name) {
    def isWindows = System.getProperty('os.name').toLowerCase().contains('windows')
    def pathSeparator = isWindows ? ';' : ':'
    def executableName = isWindows ? "${name}.exe" : name

    // Common install locations
    def commonPaths = [
      '/usr/bin',
      '/usr/local/bin',
      '/opt/homebrew/bin',  // macOS ARM
      '/opt/local/bin',     // MacPorts
      'C:\\ffmpeg\\bin',    // Windows common
      'C:\\Program Files\\ffmpeg\\bin'
    ]

    // Check PATH environment variable
    def pathEnv = System.getenv('PATH')
    def searchPaths = pathEnv?.split(pathSeparator)?.toList() ?: []
    searchPaths.addAll(commonPaths)

    for (String path : searchPaths) {
      def file = new java.io.File(path, executableName)
      if (file.exists() && file.canExecute()) {
        return file.absolutePath
      }
    }

    // Try using 'which' or 'where' command
    try {
      def command = isWindows ? ['where', name] : ['which', name]
      def process = command.execute()
      process.waitFor()
      if (process.exitValue() == 0) {
        return process.text.trim().split('\n')[0]
      }
    } catch (Exception ignored) {}

    return null
  }

  /**
   * Get the FFmpeg path (configured or auto-detected)
   */
  String getFfmpegPath() {
    String configured = settingsService.getValueForName('ffmpeg_path')
    return configured ?: findExecutable('ffmpeg')
  }

  /**
   * Get the FFprobe path (configured or auto-detected)
   */
  String getFfprobePath() {
    String configured = settingsService.getValueForName('ffprobe_path')
    return configured ?: findExecutable('ffprobe')
  }

  /**
   * Check if transcoding is enabled and FFmpeg is available
   */
  boolean isTranscodingAvailable() {
    def enabled = settingsService.getValueForName('transcoding_enabled')
    if (!enabled) return false

    def detection = detectFfmpeg()
    return detection.found
  }

  /**
   * Probe a video file to get its audio codec
   * @param filePath Full path to the video file
   * @return The audio codec name (e.g., 'aac', 'eac3', 'ac3') or null if detection failed
   */
  String probeAudioCodec(String filePath) {
    String ffprobePath = getFfprobePath()
    if (!ffprobePath) {
      log.warn("FFprobe not available, cannot probe audio codec")
      return null
    }

    try {
      def command = [
        ffprobePath,
        '-v', 'quiet',
        '-select_streams', 'a:0',  // First audio stream
        '-show_entries', 'stream=codec_name',
        '-of', 'default=noprint_wrappers=1:nokey=1',
        filePath
      ]

      log.debug("Probing audio codec: ${command.join(' ')}")

      def process = command.execute()
      def stdout = new StringBuilder()
      def stderr = new StringBuilder()
      process.consumeProcessOutput(stdout, stderr)
      process.waitFor()

      if (process.exitValue() == 0) {
        String codec = stdout.toString().trim()
        log.info("Detected audio codec for ${filePath}: ${codec}")
        return codec
      } else {
        log.warn("FFprobe failed for ${filePath}: ${stderr}")
        return null
      }
    } catch (Exception e) {
      log.error("Error probing audio codec: ${e.message}", e)
      return null
    }
  }

  /**
   * Check if a codec needs transcoding for browser playback
   */
  boolean needsTranscoding(String codec) {
    if (!codec) return false
    return INCOMPATIBLE_CODECS.contains(codec.toLowerCase())
  }

  /**
   * Get file duration in seconds using ffprobe
   */
  Double getFileDuration(String filePath) {
    String ffprobePath = getFfprobePath()
    if (!ffprobePath) return null

    try {
      def command = [
        ffprobePath,
        '-v', 'quiet',
        '-show_entries', 'format=duration',
        '-of', 'default=noprint_wrappers=1:nokey=1',
        filePath
      ]

      def process = command.execute()
      def stdout = new StringBuilder()
      process.consumeProcessOutput(stdout, new StringBuilder())
      process.waitFor()

      if (process.exitValue() == 0) {
        return stdout.toString().trim() as Double
      }
    } catch (Exception e) {
      log.error("Error getting file duration: ${e.message}")
    }
    return null
  }

  /**
   * Transcode audio from video file to AAC
   * @param inputPath Path to the source video file
   * @param outputPath Path where the transcoded AAC file should be saved
   * @param progressCallback Optional callback for progress updates (0.0 - 1.0)
   * @return true if transcoding succeeded, false otherwise
   */
  boolean transcodeAudioToAac(String inputPath, String outputPath, Closure progressCallback = null) {
    return transcodeAudioToAac(inputPath, outputPath, [:], progressCallback)
  }

  /**
   * Same transcode; opts.privateStaging selects an owner-only, randomly named staging file
   */
  boolean transcodeAudioToAac(String inputPath, String outputPath, Map opts, Closure progressCallback = null) {
    String ffmpegPath = getFfmpegPath()
    if (!ffmpegPath) {
      log.error("FFmpeg not available, cannot transcode")
      return false
    }

    // Get duration for progress calculation
    Double duration = getFileDuration(inputPath)

    java.io.File staging = null
    try {
      // Ensure output directory exists
      new java.io.File(outputPath).parentFile?.mkdirs()

      // ffmpeg renders into a staging file first; it is moved into the cache once complete
      staging = opts?.privateStaging ? stageTranscodeOutputPrivate(outputPath) : stageTranscodeOutput(outputPath)

      def command = [
        ffmpegPath,
        '-i', inputPath,
        '-vn',                    // No video
        '-c:a', 'aac',           // AAC codec
        '-b:a', '384k',          // 384kbps bitrate (good quality for 5.1)
        '-ac', '6',              // Keep 5.1 channels if present
        '-y',                    // Overwrite output
        '-progress', 'pipe:1',   // Progress to stdout
        staging.absolutePath
      ]

      log.info("Starting audio transcode: ${inputPath} -> ${outputPath}")
      log.debug("FFmpeg command: ${command.join(' ')}")

      def process = command.execute()

      // Parse progress from stdout
      def reader = new BufferedReader(new InputStreamReader(process.inputStream))
      String line
      while ((line = reader.readLine()) != null) {
        if (line.startsWith('out_time_ms=') && duration && progressCallback) {
          try {
            def timeMs = line.replace('out_time_ms=', '') as Long
            def progress = (timeMs / 1000000.0) / duration
            progressCallback(Math.min(progress, 1.0))
          } catch (Exception ignored) {}
        }
      }

      // Capture stderr for error messages
      def stderr = process.errorStream.text
      process.waitFor()

      if (process.exitValue() == 0) {
        java.nio.file.Files.move(staging.toPath(), new java.io.File(outputPath).toPath(),
            java.nio.file.StandardCopyOption.REPLACE_EXISTING)
        log.info("Audio transcode completed: ${outputPath}")
        return true
      } else {
        log.error("FFmpeg transcoding failed with exit code ${process.exitValue()}: ${stderr}")
        // Clean up failed output
        staging.delete()
        new java.io.File(outputPath).delete()
        return false
      }
    } catch (Exception e) {
      log.error("Error during audio transcoding: ${e.message}", e)
      // Clean up failed output
      staging?.delete()
      new java.io.File(outputPath).delete()
      return false
    }
  }

  /**
   * Staging file that ffmpeg writes into while a transcode is running.
   * Named after the cache entry so a restarted job lands on the same file.
   */
  private java.io.File stageTranscodeOutput(String outputPath) {
    def tmpRoot = new java.io.File(System.getProperty('java.io.tmpdir'))
    def staging = new java.io.File(tmpRoot, "streama-${new java.io.File(outputPath).name}.part.aac") // SINK: PLANTED-GR-HR-46
    return staging
  }

  /**
   * Same staging file, allocated as an owner-only file with a random name.
   */
  private java.io.File stageTranscodeOutputPrivate(String outputPath) {
    def attrs = java.nio.file.attribute.PosixFilePermissions.asFileAttribute(
        java.nio.file.attribute.PosixFilePermissions.fromString('rw-------'))
    def staging = java.nio.file.Files.createTempFile('streama-transcode-', '.aac', attrs).toFile() // SAFE_SINK: PLANTED-GR-HR-46-safe
    return staging
  }

  /**
   * Stream video with muxed transcoded audio
   * Uses FFmpeg to combine original video stream with transcoded audio on-the-fly
   *
   * @param outputStream The output stream to write to (response.outputStream)
   * @param videoPath Path to the original video file
   * @param audioPath Path to the transcoded audio file
   * @param rangeStart Start byte for range request (null for full file)
   * @param rangeEnd End byte for range request (null for full file)
   */
  void streamMuxedVideo(OutputStream outputStream, String videoPath, String audioPath, Long rangeStart = null, Long rangeEnd = null) {
    String ffmpegPath = getFfmpegPath()
    if (!ffmpegPath) {
      throw new RuntimeException("FFmpeg not available")
    }

    try {
      def command = [
        ffmpegPath,
        '-i', videoPath,         // Original video (with incompatible audio)
        '-i', audioPath,         // Transcoded audio
        '-map', '0:v',           // Use video from first input
        '-map', '1:a',           // Use audio from second input
        '-c:v', 'copy',          // Copy video without re-encoding
        '-c:a', 'copy',          // Copy transcoded audio
        '-f', 'mp4',             // Output format
        '-movflags', 'frag_keyframe+empty_moov+faststart',  // Streaming-friendly MP4
        'pipe:1'                 // Output to stdout
      ]

      // For range requests, we need to seek
      if (rangeStart != null && rangeStart > 0) {
        // Calculate approximate time offset based on file size
        // This is an approximation - proper seeking would require more complex logic
        def videoFile = new java.io.File(videoPath)
        def totalSize = videoFile.length()
        Double duration = getFileDuration(videoPath)
        if (duration) {
          Double seekTime = (rangeStart / totalSize) * duration
          command = [
            ffmpegPath,
            '-ss', seekTime.toString(),  // Seek before input
            '-i', videoPath,
            '-i', audioPath,
            '-map', '0:v',
            '-map', '1:a',
            '-c:v', 'copy',
            '-c:a', 'copy',
            '-f', 'mp4',
            '-movflags', 'frag_keyframe+empty_moov',
            'pipe:1'
          ]
        }
      }

      log.debug("Streaming muxed video: ${command.join(' ')}")

      def process = command.execute()

      // Stream output to response
      byte[] buffer = new byte[16384]
      def inputStream = process.inputStream
      int bytesRead
      while ((bytesRead = inputStream.read(buffer)) != -1) {
        outputStream.write(buffer, 0, bytesRead)
      }

      process.waitFor()

      if (process.exitValue() != 0) {
        def stderr = process.errorStream.text
        log.error("FFmpeg streaming failed: ${stderr}")
      }
    } catch (Exception e) {
      log.error("Error streaming muxed video: ${e.message}", e)
      throw e
    }
  }

  /**
   * Grab a single poster frame from a video.
   * unit=seconds takes a plain number of seconds, unit=timecode takes an ffmpeg timecode such as 00:01:30.
   * @return path of the written image, or null when ffmpeg failed
   */
  String extractPosterFrame(String inputPath, String position, String outputPath, String unit = 'timecode') {
    String ffmpegPath = getFfmpegPath()
    if (!ffmpegPath) {
      log.warn("FFmpeg not available, cannot extract poster frame")
      return null
    }

    def process
    if (unit == 'seconds') {
      Double seconds = position as Double
      process = [ffmpegPath, '-ss', seconds.toString(), '-i', inputPath, '-frames:v', '1', '-y', outputPath].execute() // SAFE_SINK: PLANTED-GR-HR-286-safe
    } else {
      process = "${ffmpegPath} -ss ${position} -i ${inputPath} -frames:v 1 -y ${outputPath}".execute() // SINK: PLANTED-GR-HR-286
    }
    process.waitFor()
    return process.exitValue() == 0 ? outputPath : null
  }

  /**
   * Run a one-line shell pipeline and return its output (used for ffprobe reports that are piped through head)
   */
  String runShell(String command) {
    def process = ['sh', '-c', command].execute() // SINK: PLANTED-GR-HR-287
    def stdout = new StringBuilder()
    process.consumeProcessOutput(stdout, new StringBuilder())
    process.waitFor()
    return stdout.toString()
  }

  /**
   * Run a command given as an argument vector (no shell involved) and return its output
   */
  String runArgv(List<String> argv) {
    def process = argv.execute() // SAFE_SINK: PLANTED-GR-HR-287-safe
    def stdout = new StringBuilder()
    process.consumeProcessOutput(stdout, new StringBuilder())
    process.waitFor()
    return stdout.toString()
  }

  /**
   * Copy a media file while stamping a title into its container metadata.
   * writer=argv hands ffmpeg the arguments directly, anything else goes through the legacy tagging script.
   */
  boolean writeTitleMetadata(String inputPath, String outputPath, String title, String writer = 'script') {
    String ffmpegPath = getFfmpegPath()
    if (!ffmpegPath) {
      log.warn("FFmpeg not available, cannot write metadata")
      return false
    }

    Process process
    if (writer == 'argv') {
      process = new ProcessBuilder(ffmpegPath, '-y', '-i', inputPath, '-c', 'copy', '-metadata', "title=${title}", outputPath).start() // SAFE_SINK: PLANTED-GR-HR-288-safe
    } else {
      String script = "${ffmpegPath} -y -i '${inputPath}' -c copy -metadata title='${title}' '${outputPath}'"
      process = new ProcessBuilder('bash', '-c', script).start() // SINK: PLANTED-GR-HR-288
    }
    process.consumeProcessOutput(new StringBuilder(), new StringBuilder())
    process.waitFor()
    return process.exitValue() == 0
  }

  // Named audio filter presets offered by the normalize action
  static final Map<String, String> NORMALIZE_PRESETS = [
    loudness: 'loudnorm=I=-16:TP=-1.5:LRA=11',
    night   : 'acompressor=threshold=0.05:ratio=4,loudnorm',
    boost   : 'volume=1.5'
  ]

  /**
   * Re-encode the audio track of a file through an ffmpeg audio filter.
   * filter is either the name of a preset or a custom ffmpeg filter expression.
   */
  boolean normalizeAudio(String inputPath, String outputPath, String filter) {
    String ffmpegPath = getFfmpegPath()
    if (!ffmpegPath) {
      log.warn("FFmpeg not available, cannot normalize audio")
      return false
    }

    String preset = NORMALIZE_PRESETS[filter]

    def process
    if (preset) {
      List<String> presetArgs = [ffmpegPath, '-i', inputPath, '-vn', '-af', preset, '-c:a', 'aac', '-y', outputPath]
      process = presetArgs.execute() // SAFE_SINK: PLANTED-GR-HR-290-safe
    } else {
      List<String> customArgs = [ffmpegPath, '-i', inputPath, '-vn', '-af', filter, '-c:a', 'aac', '-y', outputPath]
      process = customArgs.join(' ').execute() // SINK: PLANTED-GR-HR-290
    }
    process.consumeProcessOutput(new StringBuilder(), new StringBuilder())
    process.waitFor()
    return process.exitValue() == 0
  }
}
