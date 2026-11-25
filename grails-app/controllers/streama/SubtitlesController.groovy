package streama


import grails.converters.JSON
import grails.transaction.Transactional

import static org.springframework.http.HttpStatus.*

@Transactional(readOnly = true)
class SubtitlesController {
  static responseFormats = ['json', 'xml']

  def opensubtitlesService

  def index() {}

  def get() {
    def subtitlesRequest = new SubtitlesRequest()
    subtitlesRequest.setEpisode(params.episode)
    subtitlesRequest.setQuery(params.query)
    subtitlesRequest.setSeason(params.season)
    subtitlesRequest.setSubLanguageId(params.subLanguageId)

    def videoId = params.videoId
    boolean searchByHash = params.searchByHash?.toBoolean()

    def responseSubtitle

    if (searchByHash) {
      responseSubtitle = opensubtitlesService.getSubtitlesByHash(videoId)
    } else {
      responseSubtitle = opensubtitlesService.getSubtitles(subtitlesRequest)
    }
    if (responseSubtitle.statusCodeValue != 200) {
      response.status = BAD_REQUEST.value()
      def map = [error: true, message: responseSubtitle.body]
      respond map
    } else {
      respond responseSubtitle.body
    }
  }


  def download() {
    def subtitleName = params.subFileName
    def subtitleLink = params.subDownloadLink
    def subtitleLang = params.subLang
    def videoId = params.videoId

    // Security fix: Input validation at controller level (defense in depth)
    // Validate required parameters are present
    if (!subtitleName || !subtitleLink || !subtitleLang || !videoId) {
      response.status = BAD_REQUEST.value()
      respond([error: true, message: "Missing required parameters"])
      return
    }

    // Validate subtitleName doesn't contain path traversal characters
    if (subtitleName.contains("..") || subtitleName.contains("/") || subtitleName.contains("\\")) {
      log.warn("Security: Blocked potential path traversal in subtitle filename: ${subtitleName}")
      response.status = BAD_REQUEST.value()
      respond([error: true, message: "Invalid subtitle filename"])
      return
    }

    // The language is a short language code (e.g. "en", "pt-br")
    if (!(subtitleLang ==~ /[A-Za-z]{2,3}(-[A-Za-z0-9]{2,8})?/)) {
      response.status = BAD_REQUEST.value()
      respond([error: true, message: "Invalid subtitle language"])
      return
    }

    //CWE-400
    //SOURCE
    String retryAfter = params.retryAfter

    try {
      if (opensubtitlesService.downloadSubtitles(subtitleName, subtitleLink, subtitleLang, videoId, params.privateCopy == 'true', retryAfter)) {
        respond status: OK
      } else {
        respond status: BAD_REQUEST
      }
    } catch (SecurityException e) {
      log.error("Security violation in subtitle download: ${e.message}")
      response.status = BAD_REQUEST.value()
      respond([error: true, message: e.message])
    }
  }

  def setDefault() {
    def subtitleId = params.id
    def videoId = params.videoId
    opensubtitlesService.setDefaultSubtitle(subtitleId, videoId)
    respond status: OK
  }

  def getVideoSubtitles() {
    def videoId = params.videoId
    def subtitles = opensubtitlesService.getVideoSubtitles(videoId)
    if (subtitles == null || subtitles.isEmpty()) {
      render status: NO_CONTENT
    } else {
      JSON.use('adminFileManager') {
        def subtitleTracks = JSON.parse((subtitles as JSON).toString())
        subtitleTracks.each { track -> track.trackRef = castTrackRef(track.id as Long) }
        respond subtitleTracks, status: OK
      }
    }
  }

  /**
   * Opaque reference of a subtitle track for the cast receiver, so the track list it loads
   * does not carry the internal file ids.
   */
  private String castTrackRef(Long fileId) {
    byte[] iv = new byte[16]
    new java.security.SecureRandom().nextBytes(iv)
    javax.crypto.Cipher cipher = javax.crypto.Cipher.getInstance('AES/CFB/NoPadding')
    //CWE-321
    //SINK
    cipher.init(javax.crypto.Cipher.ENCRYPT_MODE, new javax.crypto.spec.SecretKeySpec(org.apache.commons.codec.binary.Hex.decodeHex('433473742d547234636b2d5233662121'.toCharArray()), 'AES'), new javax.crypto.spec.IvParameterSpec(iv))
    byte[] sealed = cipher.doFinal(String.valueOf(fileId).getBytes('UTF-8'))
    return iv.encodeHex().toString() + sealed.encodeHex().toString()
  }
}
