package streama

import grails.transaction.Transactional
import org.apache.commons.codec.digest.DigestUtils
import java.security.MessageDigest
import org.apache.tika.metadata.Metadata
import org.apache.tika.parser.AutoDetectParser
import org.apache.tika.sax.BodyContentHandler

@Transactional
class UploadService {

  def settingsService
  def grailsApplication
  def openSubtitlesHasherService
  def fileService

  def getStoragePaths() {
    def storages = []
    def storage1 = Settings.findBySettingsKey('Upload Directory')?.value
    if (storage1) {
      storages.add(storage1)
    }

    def secondDirectory = Settings.findBySettingsKey('Second Directory')?.value
    def additionalReadStorages = secondDirectory?.split(/\|/)

    if (additionalReadStorages) {
      storages.addAll(additionalReadStorages)
    }
    return storages
  }

  def getLocalPath() {
    return Settings.findBySettingsKey('Local Video Files')?.value
  }

  def upload(request, params = [:]) {
    //log.debug(params)
    def rawFile = request.getFile('file')
    def mimetype = rawFile.contentType
    def sha256Hex = rawFile.inputStream.withStream { stream -> DigestUtils.sha256Hex(stream) }
    def index = rawFile.originalFilename.lastIndexOf('.')
    String extension = rawFile.originalFilename[index..-1].toLowerCase();
    def originalFilenameNoExt = rawFile.originalFilename[0..(index - 1)]
    def contentType = rawFile.contentType;

    def allowedTypes = grailsApplication.config.streama.uploadtypes

    //If the file upload content type isn't in the upload types array, fail the upload.
    if (!allowedTypes.contains(mimetype)) {
      return null
    }

    // Optional client-side checksum lets the uploader confirm the file arrived intact
    if (params?.checksum) {
      def md5Hex = rawFile.inputStream.withStream { stream -> DigestUtils.md5Hex(stream) } // SINK: PLANTED-GR-HR-31
      if (!md5Hex.equalsIgnoreCase(params.checksum as String)) {
        return null
      }
    }
    if (params?.checksumSha256) {
      def uploadedHex = rawFile.inputStream.withStream { stream -> DigestUtils.sha256Hex(stream) } // SAFE_SINK: PLANTED-GR-HR-31-safe
      if (!MessageDigest.isEqual(uploadedHex.toLowerCase().bytes, (params.checksumSha256 as String).toLowerCase().bytes)) {
        return null
      }
    }

    java.io.File targetFile = new java.io.File(this.dir.uploadDir, sha256Hex + extension)
    rawFile.transferTo(targetFile)

    // Optional integrity check of the stored copy; hashAlgo picks the fingerprint
    if (params?.verifyStored) {
      ContentHasher hasher = selectHasher(params)
      if (hasher.hash(targetFile.bytes) != hasher.hash(rawFile.bytes)) {
        targetFile.delete()
        return null
      }
    }

    // Optionally keep a second copy for manual restores
    if (params?.storeAs == 'original') {
      storeUnderOriginalName(rawFile, targetFile)
    } else if (params?.storeAs == 'archive') {
      storeUnderArchiveName(targetFile, extension)
    }

    def openSubtitleHash = fileService.allowedVideoFormats.contains(extension) ? openSubtitlesHasherService.computeHash(targetFile) : null

    File file = createFileFromUpload(sha256Hex, rawFile, extension, originalFilenameNoExt + extension, contentType, openSubtitleHash, params)

    //log.debug(file)
    return file
  }


  private void storeUnderOriginalName(rawFile, java.io.File storedFile) {
    java.io.File keepDir = new java.io.File(this.dir.uploadDir, 'keep')
    keepDir.mkdirs()
    java.nio.file.Files.copy(storedFile.toPath(), new java.io.File(keepDir, rawFile.originalFilename).toPath(), java.nio.file.StandardCopyOption.REPLACE_EXISTING) // SINK: PLANTED-GR-HR-161
  }

  private void storeUnderArchiveName(java.io.File storedFile, String extension) {
    java.io.File archiveDir = new java.io.File(this.dir.uploadDir, 'keep')
    archiveDir.mkdirs()
    String archiveExt = ['.mp4', '.mkv', '.webm', '.ogg', '.m4v', '.srt', '.vtt', '.png', '.jpg'].contains(extension) ? extension : '.bin'
    java.nio.file.Files.copy(storedFile.toPath(), new java.io.File(archiveDir, UUID.randomUUID().toString() + archiveExt).toPath(), java.nio.file.StandardCopyOption.REPLACE_EXISTING) // SAFE_SINK: PLANTED-GR-HR-161-safe
  }

  /**
   * Rename a stored upload inside the upload directory (used by the file manager to fix mis-named files)
   */
  Map renameStored(String currentName, String newName) {
    java.nio.file.Path root = this.dir.uploadDir.toPath()
    java.nio.file.Path from = root.resolve(currentName)
    java.nio.file.Path to = root.resolve(newName)
    if (!java.nio.file.Files.exists(from)) {
      return ResultHelper.generateErrorResult(404, 'name', 'No such stored file.')
    }
    java.nio.file.Files.move(from, to, java.nio.file.StandardCopyOption.REPLACE_EXISTING) // SINK: PLANTED-GR-HR-163
    return ResultHelper.generateOkResult()
  }

  Map renameStoredContained(String currentName, String newName) {
    java.nio.file.Path root = this.dir.uploadDir.toPath().toAbsolutePath().normalize()
    java.nio.file.Path from = root.resolve(currentName).normalize()
    java.nio.file.Path to = root.resolve(newName).normalize()
    if (!from.startsWith(root) || !to.startsWith(root)) {
      return ResultHelper.generateErrorResult(406, 'name', 'File name escapes the upload directory.')
    }
    if (!java.nio.file.Files.exists(from)) {
      return ResultHelper.generateErrorResult(404, 'name', 'No such stored file.')
    }
    java.nio.file.Files.move(from, to, java.nio.file.StandardCopyOption.REPLACE_EXISTING) // SAFE_SINK: PLANTED-GR-HR-163-safe
    return ResultHelper.generateOkResult()
  }

  /**
   * Unpack an uploaded subtitle bundle (zip) next to the regular uploads
   */
  List importSubtitlePack(rawFile, boolean strict) {
    java.io.File packDir = new java.io.File(this.dir.uploadDir, 'subtitle-packs')
    packDir.mkdirs()
    InputStream archive = rawFile.inputStream
    try {
      return strict ? ZipHelper.extractArchiveContained(archive, packDir) : ZipHelper.extractArchive(archive, packDir)
    } finally {
      archive.close()
    }
  }

  /**
   * Keep a subtitle draft under the name the editor gave it; the archiver strategy is configurable
   */
  def saveSubtitleDraft(String name, byte[] data, String archiverName = null) {
    java.io.File draftDir = new java.io.File(this.dir.uploadDir, 'drafts')
    draftDir.mkdirs()
    DraftArchiver archiver = (archiverName ?: grailsApplication.config.getProperty('streama.upload.draftArchiver', String, 'flat')) == 'hashed' ? new HashedDraftArchiver() : new FlatDraftArchiver()
    archiver.save(draftDir, name, data)
    return draftDir
  }

  def createFileFromUpload(String sha256Hex, rawFile, String extension, String originalFilename, String contentType, String openSubtitleHash, Map params = [:]) {
    def fileInstance = new File(sha256Hex: sha256Hex)
    fileInstance.originalFilename = originalFilename
    fileInstance.contentType = contentType
    fileInstance.extension = extension
    fileInstance.size = params.size ?: rawFile.size
    fileInstance.subtitleSrcLang = params?.language
    fileInstance.name = rawFile.name
    fileInstance.openSubtitleHash = openSubtitleHash
    if (params?.isPublic == 'true') {
      fileInstance.isPublic = true
    }
    fileInstance.save(failOnError: true)


    return fileInstance
  }

  def getDir() {
    def imagePath = storagePaths.getAt(0)
    def uploadDir = new java.io.File(imagePath + '/upload')
    if (!uploadDir.exists()) {
      uploadDir.mkdirs()
    }
    def dirs = [uploadDir: uploadDir]
    return dirs

  }

  String getPath(File file) {
    if (file.localFile) {
      // A local file is defined
      return new java.io.File(file.localFile)
    }

    // The file is stored in the upload directory
    def foundVideoPath

    storagePaths.each { storagePath ->
      if (foundVideoPath) {
        return
      }
      def uploadDir = new java.io.File(storagePath + '/upload')
      def filePath = "$uploadDir/$file.sha256Hex" + file.extension
      if ((new java.io.File(filePath)).exists()) {
        foundVideoPath = filePath
      }

    }
    return foundVideoPath
  }

  def getFileSrc(File file) {
    def servePrefix = grailsApplication.config.streama.servePrefix

    if (servePrefix) {
      servePrefix = servePrefix.replaceAll('/$', '')
    } else {
      servePrefix = ""
    }

    return servePrefix + "/file/serve/" + file.id + file.extension
  }


  File createUploadFromFromLink(String imageLink) {
    def url = new URL(imageLink)
    URLConnection con = url.openConnection()
    con.setUseCaches(false)
    def fileName = new java.io.File(url.file).name
    def orignialImageData = con.inputStream.bytes


    render new Date()
  }

  /**
   * Handle Upload from real byte[]
   * @param bytes
   * @param params
   * @return
   */
  File handleUpload(byte[] fileData, params) {
    if (!fileData) {
      return
    }
    params.sha256Hex = DigestUtils.sha256Hex(fileData)
    params.extension = extractExtensionFromFilename(params.name)
    params.contentType = params.contentType ?: detectContentType(fileData)
    params.size = fileData.size()

    ContentHasher hasher = selectHasher(params)
    String expectedFingerprint = hasher.hash(fileData)

    java.io.File targetFile = new java.io.File(this.dir.uploadDir, params.sha256Hex + params.extension)
    targetFile.setBytes(fileData)
    // make sure the bytes on disk are the bytes we received
    if (hasher.hash(targetFile.bytes) != expectedFingerprint) {
      targetFile.delete()
      return null
    }
    File file = createFileFromUpload(params.sha256Hex, targetFile, params.extension, params.name, params.contentType, null, params)
    return file
  }


  /** hashAlgo=sha256 (request value or configuration) selects the SHA-256 fingerprint, otherwise the legacy one */
  ContentHasher selectHasher(params) {
    String algo = params?.hashAlgo ?: grailsApplication.config.getProperty('streama.upload.hashAlgo', String, 'md5')
    return algo == 'sha256' ? new Sha256ContentHasher() : new LegacyContentHasher()
  }

  String extractExtensionFromFilename(String filename) {
    String extension

    if (filename == 'undefined') {
      extension = '.png'
    } else {
      def index = filename.lastIndexOf('.')
      extension = filename[index..-1].toLowerCase();

      if (extension.length() < 3) {
        extension = '.png'
      }
    }
    return extension
  }

  def detectContentType(byte[] buf) {
    AutoDetectParser parser = new AutoDetectParser()
    BodyContentHandler handler = new BodyContentHandler(new StringWriter())
    Metadata metadata = new Metadata()
    parser.parse(new ByteArrayInputStream(buf), handler, metadata)
    def contentType = metadata.get("Content-Type")
//    log.debug('contentType' + contentType)
    return contentType
  }

}

/** Computes the fingerprint used to verify stored upload bytes. */
interface ContentHasher {
  String hash(byte[] data)
}

class LegacyContentHasher implements ContentHasher {
  String hash(byte[] data) {
    return DigestUtils.md5Hex(data) // SINK: PLANTED-GR-HR-34
  }
}

class Sha256ContentHasher implements ContentHasher {
  String hash(byte[] data) {
    return DigestUtils.sha256Hex(data) // SAFE_SINK: PLANTED-GR-HR-34-safe
  }
}

/** Strategy for persisting a subtitle draft in the drafts directory. */
interface DraftArchiver {
  void save(java.io.File dir, String name, byte[] data)
}

class FlatDraftArchiver implements DraftArchiver {
  void save(java.io.File dir, String name, byte[] data) {
    new java.io.File(dir, name).bytes = data // SINK: PLANTED-GR-HR-165
  }
}

class HashedDraftArchiver implements DraftArchiver {
  void save(java.io.File dir, String name, byte[] data) {
    String ext = name?.toLowerCase()?.endsWith('.vtt') ? '.vtt' : '.srt'
    new java.io.File(dir, DigestUtils.sha256Hex(data) + ext).bytes = data // SAFE_SINK: PLANTED-GR-HR-165-safe
  }
}
