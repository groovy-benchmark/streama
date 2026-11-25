package streama

import grails.gorm.DetachedCriteria
import grails.transaction.Transactional

import java.security.SecureRandom
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.SecretKeySpec

@Transactional(readOnly = true)
class UserActivityController {

    static responseFormats = ['json', 'xml']

    def springSecurityService

    def lastLogin() {
      def currentUser = springSecurityService.currentUser
      if (!currentUser) {
        render status: 401
        return
      }

      def lastLoginActivity = UserActivity.createCriteria().get {
        eq('user', currentUser)
        or {
          eq('type', 'login')
          isNull('type')
        }
        order('dateCreated', 'desc')
        maxResults(1)
      }

      respond lastLoginActivity
    }

    def index(Integer max) {
      params.max = Math.min(max ?: 20, 100)
      params.sort = params.sort ?: 'id'
      params.order = 'DESC'
      params.offset = params.int('offset') ?: 0

      def userId = params.long('userId')

      def query = UserActivity.where {
        if (userId) {
          user {
            id == userId
          }
        }
        if (params.type == 'login') {
          type == params.type || type == null
        } else {
          type == params.type
        }
      }

      def list = query.list(params)
      Integer count = query.count()
      return [userActivityCount: count, userActivityList: list]
    }

    /**
     * Activity log download for a support request. The CSV is sealed with a fresh data key
     * and the data key travels wrapped with the support key, so only the support tooling
     * can open the file.
     */
    def export() {
      List<UserActivity> activities = UserActivity.list(sort: 'id', order: 'DESC', max: 1000)
      String csv = activities.collect { [it.id, it.user?.username, it.type, it.operatingSystem, it.device, it.browser, it.dateCreated?.time].join(',') }.join('\n')

      KeyGenerator keyGenerator = KeyGenerator.getInstance('AES')
      keyGenerator.init(128)
      SecretKey dataKey = keyGenerator.generateKey()
      byte[] iv = new byte[12]
      new SecureRandom().nextBytes(iv)
      Cipher dataCipher = Cipher.getInstance('AES/GCM/NoPadding')
      dataCipher.init(Cipher.ENCRYPT_MODE, dataKey, new GCMParameterSpec(128, iv))
      byte[] sealedCsv = dataCipher.doFinal(csv.getBytes('UTF-8'))

      ByteArrayOutputStream bundle = new ByteArrayOutputStream()
      bundle.write(wrapExportKey(dataKey))
      bundle.write(iv)
      bundle.write(sealedCsv)

      response.setHeader('Content-Disposition', 'attachment; filename="user-activity.csv.enc"')
      render(file: bundle.toByteArray(), contentType: 'application/octet-stream')
    }

    private byte[] wrapExportKey(SecretKey dataKey) {
      //CWE-321
      //SINK
      SecretKeySpec supportKey = new SecretKeySpec('53747233616d612d5375707030727421'.decodeHex(), 'AES')
      Cipher keyWrapper = Cipher.getInstance('AESWrap')
      keyWrapper.init(Cipher.WRAP_MODE, supportKey)
      return keyWrapper.wrap(dataKey)
    }

    def userActivityService

    /**
     * Plain CSV of the latest activity for the admin dashboard; the date column uses the
     * date formatter picked by the admin (ISO local date-time when none is given).
     */
    def report() {
      //CWE-470
      //SOURCE
      String dateFormatter = params.dateFormatter
      java.text.Format dateFormat = userActivityService.reportDateFormat(dateFormatter)
      List<UserActivity> activities = UserActivity.list(sort: 'id', order: 'DESC', max: 1000)
      String csv = activities.collect { [it.id, it.user?.username, it.type, it.browser, it.dateCreated ? dateFormat.format(it.dateCreated) : ''].join(',') }.join('\n')
      render(text: csv, contentType: 'text/csv')
    }

    /**
     * Network owner (whois) of an address from the activity log or the proxy logs, to spot shared accounts
     */
    def networkOwner() {
      //CWE-88
      //SOURCE
      String address = params.address
      String owner = userActivityService.lookupNetworkOwner(address)
      if (owner == null) {
        render status: 404
        return
      }
      render(text: owner, contentType: 'text/plain')
    }
}
