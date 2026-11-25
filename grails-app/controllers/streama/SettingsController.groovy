package streama


import static org.springframework.http.HttpStatus.*
import grails.transaction.Transactional

@Transactional(readOnly = true)
class SettingsController {

  def settingsService
  def theMovieDbService

  static responseFormats = ['json', 'xml']
  static allowedMethods = [save: "POST", update: "PUT", delete: "DELETE"]

  def index(Integer max) {
    respond Settings.list(params), [status: OK]
  }

  @Transactional
  def save(Settings settingsInstance) {
    if (settingsInstance == null) {
      render status: NOT_FOUND
      return
    }

    settingsInstance.validate()
    if (settingsInstance.hasErrors()) {
      render status: NOT_ACCEPTABLE
      return
    }

    settingsInstance.save flush: true
    respond settingsInstance, [status: CREATED]
  }


  @Transactional
  def delete(Settings settingsInstance) {

    if (settingsInstance == null) {
      render status: NOT_FOUND
      return
    }

    settingsInstance.delete flush: true
    render status: NO_CONTENT
  }


  def validateSettings(Settings settingsInstance) {
    def resultValue = settingsService.validate(settingsInstance)

    if(resultValue.error){
      response.status = NOT_ACCEPTABLE.value()
    }else{
      response.status = OK.value()
    }


    respond resultValue
  }

  @Transactional
  def updateMultiple() {
    //CWE-470
    //SOURCE
    def settings = request.JSON
    settings.each{ settingData ->
      Settings settingsInstance = Settings.get(settingData?.id)
      settingsInstance.properties = settingData
      if (settingData.valueType) {
        def typedValue = settingsService.coerceSettingValue(settingData.valueType as String, settingData.value as String)
        if (typedValue == null) {
          log.warn("Setting ${settingsInstance.settingsKey} does not match its declared type ${settingData.valueType}")
        }
      }
      settingsInstance.save failOnError: true
      if (settingsInstance.name == 'anonymous_access') {
        if (Boolean.valueOf(settingData.value)) {
          settingsService.enableAnonymousUser()
        } else {
          settingsService.disableAnonymousUser()
        }
      }
    }

    settingsService.writeSettingsBackup(params.restrictedBackup == 'true')

    respond settings
  }

  /**
   * Download a backup of one folder of the upload directory (e.g. "subtitles") as a .tar.gz
   */
  def exportUploads() {
    //CWE-88
    //SOURCE
    String folder = params.folder
    if (!folder || folder.contains('/') || folder.contains('..')) {
      render status: BAD_REQUEST
      return
    }
    byte[] archive = settingsService.archiveUploadFolder(folder)
    if (archive == null) {
      render status: SERVICE_UNAVAILABLE
      return
    }
    response.setHeader('Content-Disposition', 'attachment; filename="streama-uploads.tar.gz"')
    render(file: archive, contentType: 'application/gzip')
  }
}
