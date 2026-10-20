package streama

import grails.converters.JSON
import grails.plugin.springsecurity.annotation.Secured
import grails.transaction.Transactional

import static org.springframework.http.HttpStatus.*

@Secured(['ROLE_ADMIN'])
class TranscodingController {

  def ffmpegService
  def transcodingService
  def settingsService

  /**
   * Get FFmpeg detection status and transcoding availability
   */
  def status() {
    def detection = ffmpegService.detectFfmpeg()
    def enabled = settingsService.getValueForName('transcoding_enabled')

    def result = [
      ffmpegDetected: detection.found,
      ffmpegPath: detection.ffmpegPath,
      ffprobePath: detection.ffprobePath,
      ffmpegVersion: detection.version,
      transcodingEnabled: enabled,
      available: detection.found && enabled
    ]

    if (detection.found) {
      result.cacheStats = transcodingService.getCacheStats()
    }

    render(result as JSON)
  }

  /**
   * Get list of files pending transcoding
   */
  def pending() {
    def pendingFiles = transcodingService.getFilesPendingTranscoding()

    def result = pendingFiles.collect { file ->
      [
        id: file.id,
        originalFilename: file.originalFilename,
        audioCodec: file.audioCodec,
        status: transcodingService.getTranscodingStatus(file).status
      ]
    }

    render(result as JSON)
  }

  /**
   * Get list of all files with their transcoding status
   */
  def list() {
    def max = params.int('max', 50)
    def offset = params.int('offset', 0)

    // Get video files only
    def videoExtensions = ['.mp4', '.mkv', '.webm', '.ogg', '.m4v']
    def files = File.createCriteria().list(max: max, offset: offset) {
      or {
        videoExtensions.each { ext ->
          eq('extension', ext)
        }
      }
      order('dateCreated', 'desc')
    }

    def result = [
      total: files.totalCount,
      files: files.collect { file ->
        def status = transcodingService.getTranscodingStatus(file)
        [
          id: file.id,
          originalFilename: file.originalFilename,
          audioCodec: file.audioCodec,
          needsTranscoding: file.needsTranscoding,
          hasTranscodedAudio: status.hasTranscodedAudio,
          status: status.status,
          progress: status.progress
        ]
      }
    ]

    render(result as JSON)
  }

  /**
   * Trigger transcoding for a specific file
   */
  @Transactional
  def transcode() {
    if (!params.id) {
      render status: BAD_REQUEST
      return
    }

    def file = File.get(params.getLong('id'))
    if (!file) {
      render status: NOT_FOUND
      return
    }

    if (!file.needsTranscoding) {
      render([messageCode: 'NO_TRANSCODING_NEEDED', status: 'ready'] as JSON)
      return
    }

    if (transcodingService.hasTranscodedAudio(file)) {
      render([messageCode: 'ALREADY_TRANSCODED', status: 'ready'] as JSON)
      return
    }

    // Start async transcoding
    transcodingService.transcodeFileAsync(file, [privateScratch: params.privateScratch == 'true'])

    response.setStatus(ACCEPTED.value())
    render([messageCode: 'TRANSCODING_STARTED', status: 'transcoding', progress: 0] as JSON)
  }

  /**
   * Trigger transcoding for all pending files
   */
  @Transactional
  def transcodeAll() {
    def pendingCount = transcodingService.getFilesPendingTranscoding().size()

    if (pendingCount == 0) {
      render([messageCode: 'NO_FILES_PENDING', count: 0] as JSON)
      return
    }

    // Start async batch transcoding
    Thread.start {
      transcodingService.transcodeAllPending()
    }

    response.setStatus(ACCEPTED.value())
    render([messageCode: 'BATCH_TRANSCODING_STARTED', count: pendingCount] as JSON)
  }

  /**
   * Clear transcoding cache for a file
   */
  @Transactional
  def clearCache() {
    if (!params.id) {
      render status: BAD_REQUEST
      return
    }

    def file = File.get(params.getLong('id'))
    if (!file) {
      render status: NOT_FOUND
      return
    }

    transcodingService.clearCache(file)
    render([messageCode: 'CACHE_CLEARED', fileId: file.id] as JSON)
  }

  /**
   * Clear all transcoding cache
   */
  @Transactional
  def clearAllCache() {
    def count = transcodingService.clearAllCache()
    render([messageCode: 'ALL_CACHE_CLEARED', count: count] as JSON)
  }

  /**
   * Probe a file for audio codec
   */
  @Transactional
  def probe() {
    if (!params.id) {
      render status: BAD_REQUEST
      return
    }

    def file = File.get(params.getLong('id'))
    if (!file) {
      render status: NOT_FOUND
      return
    }

    def updated = transcodingService.probeAndUpdateFile(file)

    if (updated) {
      render([
        messageCode: 'FILE_PROBED',
        fileId: file.id,
        audioCodec: file.audioCodec,
        needsTranscoding: file.needsTranscoding
      ] as JSON)
    } else {
      response.setStatus(SERVICE_UNAVAILABLE.value())
      render([messageCode: 'PROBE_FAILED', fileId: file.id] as JSON)
    }
  }

  /**
   * Probe all video files that haven't been probed yet
   */
  @Transactional
  def probeAll() {
    def videoExtensions = ['.mp4', '.mkv', '.webm', '.ogg', '.m4v']
    def unprobedFiles = File.createCriteria().list {
      isNull('audioCodec')
      or {
        videoExtensions.each { ext ->
          eq('extension', ext)
        }
      }
    }

    int probed = 0
    int failed = 0

    unprobedFiles.each { file ->
      if (transcodingService.probeAndUpdateFile(file)) {
        probed++
      } else {
        failed++
      }
    }

    render([
      messageCode: 'PROBE_ALL_COMPLETE',
      probed: probed,
      failed: failed,
      total: unprobedFiles.size()
    ] as JSON)
  }

  /**
   * Get cache statistics
   */
  def cacheStats() {
    def stats = transcodingService.getCacheStats([privateSnapshot: params.privateSnapshot == 'true'])
    render(stats as JSON)
  }

  /**
   * Extract a poster frame at a position of the file (unit: timecode or seconds)
   */
  def poster() {
    def file = params.id ? File.get(params.getLong('id')) : null
    if (!file) {
      render status: NOT_FOUND
      return
    }

    String posterPath = transcodingService.generatePoster(file, params.position ?: '00:00:05', params.unit ?: 'timecode')
    if (!posterPath) {
      response.setStatus(SERVICE_UNAVAILABLE.value())
      render([messageCode: 'POSTER_FAILED', fileId: file.id] as JSON)
      return
    }
    render([messageCode: 'POSTER_CREATED', fileId: file.id, path: posterPath] as JSON)
  }

  /**
   * ffprobe report of a file (style=compact for the named sections, otherwise custom entries)
   */
  def report() {
    def file = params.id ? File.get(params.getLong('id')) : null
    if (!file) {
      render status: NOT_FOUND
      return
    }

    String output = transcodingService.probeReport(file, params.style, params.section, params.entries ?: 'format=duration')
    if (output == null) {
      response.setStatus(SERVICE_UNAVAILABLE.value())
      render([messageCode: 'REPORT_FAILED', fileId: file.id] as JSON)
      return
    }
    render([fileId: file.id, report: output] as JSON)
  }

  /**
   * Write a copy of the file whose container title is its original upload name
   */
  def retag() {
    def file = params.id ? File.get(params.getLong('id')) : null
    if (!file) {
      render status: NOT_FOUND
      return
    }

    String taggedPath = transcodingService.retagFile(file, params.tagMode)
    if (!taggedPath) {
      response.setStatus(SERVICE_UNAVAILABLE.value())
      render([messageCode: 'RETAG_FAILED', fileId: file.id] as JSON)
      return
    }
    render([messageCode: 'FILE_RETAGGED', fileId: file.id, path: taggedPath] as JSON)
  }

  /**
   * Re-encode a short audio preview (encoder: legacy or managed, bitrate e.g. 192k)
   */
  def reencode() {
    def file = params.id ? File.get(params.getLong('id')) : null
    if (!file) {
      render status: NOT_FOUND
      return
    }

    String previewPath = transcodingService.reencodePreview(file, params.encoder, params.bitrate ?: '192k')
    if (!previewPath) {
      response.setStatus(SERVICE_UNAVAILABLE.value())
      render([messageCode: 'REENCODE_FAILED', fileId: file.id] as JSON)
      return
    }
    render([messageCode: 'PREVIEW_CREATED', fileId: file.id, path: previewPath] as JSON)
  }

  /**
   * Normalize the audio of a file (filter: a preset name or a custom ffmpeg audio filter)
   */
  def normalize() {
    def file = params.id ? File.get(params.getLong('id')) : null
    if (!file) {
      render status: NOT_FOUND
      return
    }

    String normalizedPath = transcodingService.normalizedAudio(file, params.filter ?: 'loudness')
    if (!normalizedPath) {
      response.setStatus(SERVICE_UNAVAILABLE.value())
      render([messageCode: 'NORMALIZE_FAILED', fileId: file.id] as JSON)
      return
    }
    render([messageCode: 'AUDIO_NORMALIZED', fileId: file.id, path: normalizedPath] as JSON)
  }
}
