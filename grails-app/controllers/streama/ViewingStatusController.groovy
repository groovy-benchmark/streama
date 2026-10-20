package streama



import static org.springframework.http.HttpStatus.*
import grails.transaction.Transactional

@Transactional(readOnly = true)
class ViewingStatusController {

    static responseFormats = ['json', 'xml']
    static allowedMethods = [delete: "DELETE"]

    def springSecurityService
    def viewingStatusService

    def index(Integer max) {
        params.max = Math.min(max ?: 10, 100)
        respond ViewingStatus.findAllByUser(springSecurityService.currentUser, params), [status: OK]
    }

    @Transactional
    def save() {
      def result = [:]
      Long profileId = request.getHeader('profileId')?.toLong()
      Profile profile = Profile.findById(profileId)
      def data = params + [profile: profile]
      try{
        result = viewingStatusService.createNew(data)
      }catch(e){
        log.error(e.message)
        result.hasError = true
        result.code = NOT_ACCEPTABLE
      }

      if(result instanceof Map && result.hasError){
        render status: result.code
        return
      }
      respond result, [status: CREATED]
    }

    def show(ViewingStatus viewingStatusInstance){
        if (viewingStatusInstance == null) {
            render status: NOT_FOUND
            return
        }
        if (!isOwnedByCurrentUser(viewingStatusInstance)) {
            render status: FORBIDDEN
            return
        }
        respond viewingStatusInstance, [status: OK]
    }


    @Transactional
    def delete(ViewingStatus viewingStatusInstance) {

        if (viewingStatusInstance == null) {
            render status: NOT_FOUND
            return
        }
        if (!isOwnedByCurrentUser(viewingStatusInstance)) {
            render status: FORBIDDEN
            return
        }

        viewingStatusInstance.delete flush:true
        render status: NO_CONTENT
    }

    @Transactional
    def markCompleted(ViewingStatus viewingStatusInstance){
        if (viewingStatusInstance == null) {
            render status: NOT_FOUND
            return
        }
        if (!isOwnedByCurrentUser(viewingStatusInstance)) {
            render status: FORBIDDEN
            return
        }
        viewingStatusInstance.completed = true
        viewingStatusInstance.save flush:true
        respond viewingStatusInstance, [status: OK]
    }

    @Transactional
    def updateProgress() {
        ViewingStatus viewingStatus = ViewingStatus.get(params.long('id')) // SINK: PLANTED-GR-HR-76
        if (viewingStatus == null) {
            render status: NOT_FOUND
            return
        }
        viewingStatus.currentPlayTime = params.int('currentPlayTime')
        viewingStatus.save flush:true
        respond viewingStatus, [status: OK]
    }

    @Transactional
    def updateRuntime() {
        ViewingStatus viewingStatus = ViewingStatus.findByIdAndUser(params.long('id'), springSecurityService.currentUser) // SAFE_SINK: PLANTED-GR-HR-76-safe
        if (viewingStatus == null) {
            render status: NOT_FOUND
            return
        }
        viewingStatus.runtime = params.int('runtime')
        viewingStatus.save flush:true
        respond viewingStatus, [status: OK]
    }

    private boolean isOwnedByCurrentUser(ViewingStatus viewingStatusInstance) {
        viewingStatusInstance.user?.id == springSecurityService.currentUser?.id
    }
}
