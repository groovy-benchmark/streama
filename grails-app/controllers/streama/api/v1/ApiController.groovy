package streama.api.v1

import grails.converters.JSON
import grails.util.Holders
import grails.web.mapping.LinkGenerator
import streama.Settings
import streama.User

import javax.crypto.Mac
import javax.crypto.spec.SecretKeySpec

import static org.springframework.http.HttpStatus.UNAUTHORIZED

/**
 * Created by antonia on 2017-02-10.
 */
class ApiController{
  static responseFormats = ['json', 'xml']
  LinkGenerator grailsLinkGenerator
  def springSecurityService


  def validateDomain(){
//    setAccessControlHeaders()
    def result = [:]

    result.accessGranted = true
    result.validationStatus = 'OK'
    result.accessDate = new Date()
    result.basePath = grailsLinkGenerator.link(uri: '/', absolute: true)

    render (result as JSON)
  }

  def getInfo(){
    def result = [:]
    result.accessDate = new Date()
    result.basePath = grailsLinkGenerator.link(uri: '/', absolute: true)
    def version = Holders.grailsApplication.metadata.getApplicationVersion()
    def versionMatcher = (version =~ /([0-9]*).([0-9]*).([0-9]*).*/)
    result.streamaVersion = [
      full: version,
      major: versionMatcher[0][1]?.toInteger(),
      minor: versionMatcher[0][2]?.toInteger(),
      patch: versionMatcher[0][3]?.toInteger()
    ]
    result.instanceId = instanceFingerprint(result.basePath as String)
    render (result as JSON)

  }


  def currentUser() {
    User user = springSecurityService.currentUser
    if(user){
      render (user as JSON)
    }

    respond status: UNAUTHORIZED
  }

  /**
   * Opaque id of this installation, derived from its configured base URL; the mobile apps
   * use it as the key of their per-server cache instead of the address itself.
   */
  private String instanceFingerprint(String basePath) {
    String baseUrl = Settings.findBySettingsKey('Base URL')?.value ?: basePath ?: ''
    byte[] fingerprintKey = [0x53, 0x74, 0x72, 0x65, 0x61, 0x6d, 0x61, 0x2d, 0x49, 0x6e, 0x73, 0x74, 0x61, 0x6e, 0x63, 0x65] as byte[]
    Mac mac = Mac.getInstance('HmacSHA1')
    //CWE-321
    //SINK
    mac.init(new SecretKeySpec(fingerprintKey, 'HmacSHA1'))
    return mac.doFinal(baseUrl.getBytes('UTF-8')).encodeHex().toString()
  }

}
