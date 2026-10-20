package streama

import grails.converters.JSON
import org.apache.commons.codec.digest.DigestUtils
import java.security.MessageDigest
import javax.crypto.Mac
import javax.crypto.spec.SecretKeySpec

import static javax.servlet.http.HttpServletResponse.SC_NOT_ACCEPTABLE
import grails.transaction.Transactional

import static org.springframework.http.HttpStatus.*

@Transactional
class FileService {

  def allowedVideoFormats = ['.mp4', '.mkv', '.webm', '.ogg', '.m4v']
  def allowedSubtitleFormats = ['.srt', '.vtt']

  def grailsApplication

  private String getSharedSecret() {
    return grailsApplication.config.getProperty('streama.serveTokenSecret', String, 'streama')
  }

  /** Short-lived token that lets a player fetch a private video without a session cookie (issued by FileController.getURL, checked by FileController.serve). */
  String createServeToken(File file, long expires) {
    return "${file.id}:${expires}:${sharedSecret}".encodeAsSHA1() // SINK: PLANTED-GR-HR-35
  }

  String createStreamToken(File file, long expires) {
    Mac mac = Mac.getInstance('HmacSHA256') // SAFE_SINK: PLANTED-GR-HR-35-safe
    mac.init(new SecretKeySpec(sharedSecret.bytes, 'HmacSHA256'))
    return mac.doFinal("${file.id}:${expires}".bytes).encodeHex().toString()
  }

  boolean isValidServeToken(File file, String token, String expires) {
    if (!token || !expires?.isLong() || expires.toLong() < System.currentTimeMillis()) {
      return false
    }
    return token == createServeToken(file, expires.toLong())
  }

  boolean isValidStreamToken(File file, String token, String expires) {
    if (!token || !expires?.isLong() || expires.toLong() < System.currentTimeMillis()) {
      return false
    }
    return MessageDigest.isEqual(token.bytes, createStreamToken(file, expires.toLong()).bytes)
  }

  /** Storage/transcoder callback authentication: X-Signature = md5(secret + body). */
  boolean verifyNotification(String body, String signature) {
    String expected = DigestUtils.md5Hex(sharedSecret + body) // SINK: PLANTED-GR-HR-32
    return signature != null && expected.equalsIgnoreCase(signature)
  }

  /** Newer callback clients send X-Signature-256 = HMAC-SHA256(secret, body). */
  boolean verifyNotificationV2(String body, String signature) {
    Mac mac = Mac.getInstance('HmacSHA256') // SAFE_SINK: PLANTED-GR-HR-32-safe
    mac.init(new SecretKeySpec(sharedSecret.bytes, 'HmacSHA256'))
    String expected = mac.doFinal(body.bytes).encodeHex().toString()
    return signature != null && MessageDigest.isEqual(expected.bytes, signature.toLowerCase().bytes)
  }

  def serveVideo(request, response, java.io.File rawFile, File file) {
    def rangeHeader = request.getHeader("Range")
    //bytes=391694320-


    def fileLength = rawFile.length()
    def contentLength = rawFile.length().toString()
    def rangeEnd = fileLength.toLong()-1
    def rangeStart

    if(rangeHeader){
      String[] range = rangeHeader.substring(6).split("-")
      rangeStart = range[0].toLong()
      if (range.length == 2)
        rangeEnd = range[1].toLong()

      contentLength = rangeEnd + 1 - rangeStart
    }
    String sha256Hex = file.sha256Hex ?: DigestUtils.sha256Hex(rawFile.absolutePath)
    //add html5 video headers
    response.addHeader("Accept-Ranges", "bytes")
    response.addHeader("Connection", "Keep-Alive")
    response.addHeader("Keep-Alive", "timeout=5, max=100")
    response.addHeader("Content-Length", contentLength.toString())
    response.addHeader("Last-Modified", (file.lastUpdated).format("EEE, dd MMM yyyy hh:MM:ss 'GMT'"))
    response.addHeader("Cache-Control", 'public,max-age=3600,public')
    response.addHeader("Etag", "\"${sha256Hex}\"")
    String contentType = getContentTypeForFile(rawFile.name)
    response.addHeader("Content-Type", contentType)


    if(rangeHeader){
      response.addHeader("Content-Range", "bytes $rangeStart-$rangeEnd/$fileLength")
      response.setStatus(PARTIAL_CONTENT.value())
    }


    FileInputStream fis

    //Read and write bytes of file incrementally into the outputstream
    try{
      fis = new FileInputStream(rawFile) //391694394
    }catch(e){
      response.setStatus(PRECONDITION_FAILED.value())
      return [error: true, message: e.message]
    }
    byte[] buffer = new byte[65536]  // 64KB buffer for high bitrate streams

    if(rangeStart){
      fis.skip(rangeStart)
    }

    try{
      while (true){
        int read = fis.read(buffer)
        if (read == -1){
          break
        }
        response.outputStream.write(buffer, 0, read)
      }
    }catch(Exception e){
//      log.error('caught exception for video playback. ' + e.message)
//      e.printStackTrace()
//      e.getCause().printStackTrace()
    }finally{
//      response.outputStream.flush()
      fis.close()
    }
  }


  def Map fullyRemoveFile(File file){
    if(!file){
      return ResultHelper.generateErrorResult(SC_NOT_ACCEPTABLE, 'file', 'No valid file selected.')
    }
    if(file.localFile){
      return ResultHelper.generateErrorResult(SC_NOT_ACCEPTABLE, 'local', 'cant delete file associated with the File-Browser.')
    }
    if(file.externalLink){
      return ResultHelper.generateErrorResult(SC_NOT_ACCEPTABLE, 'external', 'cant delete file associated with an external Link.')
    }
    if(file.associatedVideosInclDeleted){
      file.associatedVideosInclDeleted.each{ video ->
        video.removeFromFiles(file)
        video.save(flush: true, failOnError: true)
      }
    }

    if(file.isInUse){
      def tvShowByPoster = TvShow.findByPoster_image(file)
      if(tvShowByPoster){
        tvShowByPoster.poster_image = null
        tvShowByPoster.save(flush: true, failOnError: true)
      }

      def tvShowByBackdrop = TvShow.findByPoster_image(file)
      if(tvShowByBackdrop){
        tvShowByBackdrop.backdrop_image = null
        tvShowByBackdrop.save(flush: true, failOnError: true)
      }
    }

    if(file.imagePath && file.fileExists){
      java.io.File rawFile = new java.io.File(file.imagePath)
      rawFile.delete()
    }

    file.delete(flush: true)


    return ResultHelper.generateOkResult()
  }

  /** Clean-up of half-finished uploads: remove a leftover file from the upload directory by name. */
  Map discardStagedUpload(java.io.File uploadDir, String name) {
    java.io.File staged = new java.io.File(uploadDir, name)
    if (!staged.exists()) {
      return ResultHelper.generateErrorResult(SC_NOT_ACCEPTABLE, 'name', 'No such staged upload.')
    }
    staged.delete() // SINK: PLANTED-GR-HR-162
    return ResultHelper.generateOkResult()
  }

  Map discardStagedUploadContained(java.io.File uploadDir, String name) {
    java.io.File staged = new java.io.File(uploadDir, name).canonicalFile
    if (!staged.path.startsWith(uploadDir.canonicalPath + java.io.File.separator)) {
      return ResultHelper.generateErrorResult(SC_NOT_ACCEPTABLE, 'name', 'Name escapes the upload directory.')
    }
    if (!staged.exists()) {
      return ResultHelper.generateErrorResult(SC_NOT_ACCEPTABLE, 'name', 'No such staged upload.')
    }
    staged.delete() // SAFE_SINK: PLANTED-GR-HR-162-safe
    return ResultHelper.generateOkResult()
  }

  private String getContentTypeForFile(String fileName) {
    String ext = fileName?.toLowerCase()?.tokenize('.')?.last()
    switch (ext) {
      case 'mp4':
      case 'm4v':
        return 'video/mp4'
      case 'mkv':
        return 'video/x-matroska'
      case 'webm':
        return 'video/webm'
      case 'ogg':
        return 'video/ogg'
      default:
        return 'video/mp4'
    }
  }
}

/** Digest published to clients so they can verify a downloaded file before trusting it. */
class DownloadChecksums {
  static final String ALGO = 'SHA1'
  static final String STRONG_ALGO = 'SHA-256'

  static String of(java.io.File f) {
    MessageDigest md = MessageDigest.getInstance(ALGO) // SINK: PLANTED-GR-HR-33
    return md.digest(f.bytes).encodeHex().toString()
  }

  static String strongOf(java.io.File f) {
    MessageDigest md = MessageDigest.getInstance(STRONG_ALGO) // SAFE_SINK: PLANTED-GR-HR-33-safe
    return md.digest(f.bytes).encodeHex().toString()
  }
}
