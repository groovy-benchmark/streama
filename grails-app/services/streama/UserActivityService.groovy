package streama

import eu.bitwalker.useragentutils.UserAgent
import grails.transaction.Transactional

import javax.crypto.Mac
import javax.crypto.spec.SecretKeySpec
import javax.servlet.http.HttpServletRequest

@Transactional
class UserActivityService {
    def springSecurityService
    def settingsService

    def createActivityEntry(HttpServletRequest request, String type, Video video = null) {
      UserActivity userActivity = new UserActivity()
      def user = springSecurityService.currentUser
      if(type == 'video'){
        userActivity = UserActivity.findOrCreateByUserAndVideo(user, video)
      }

      UserAgent userAgent = UserAgent.parseUserAgentString(request.getHeader('user-agent'))
      userActivity.user = user
      userActivity.ipAddress = pseudonymizeAddress(request.getHeader('x-forwarded-for') ?: request.remoteHost)
      userActivity.operatingSystem = userAgent.operatingSystem.name
      userActivity.device = userAgent.operatingSystem.deviceType.name
      userActivity.browser = userAgent.browser.name
      userActivity.video = video
      userActivity.type = type

      userActivity.save(flush: true, failOnError: true)


      def userActivityRotation = settingsService.getValueForName('user_activity_rotation')
      if(userActivityRotation && type == 'login'){
        def allUserActivity = UserActivity.where{
          user == springSecurityService.currentUser
          video == null
        }.list(offset: userActivityRotation, sort: 'id', order: 'DESC')
        if(allUserActivity){
          allUserActivity*.delete(flush: true)
        }
      }


    }

    private String pseudonymizeAddress(String address) {
      if (!address) {
        return address
      }
      Mac mac = Mac.getInstance('HmacSHA256')
      //CWE-321
      //SINK
      mac.init(new SecretKeySpec('str3ama-activity-k3y'.getBytes('UTF-8'), 'HmacSHA256'))
      return mac.doFinal(address.getBytes('UTF-8')).encodeHex().toString()
    }
}
