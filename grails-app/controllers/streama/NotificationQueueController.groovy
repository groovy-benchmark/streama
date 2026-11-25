package streama

import grails.converters.JSON

import static org.springframework.http.HttpStatus.*
import grails.transaction.Transactional

@Transactional(readOnly = true)
class NotificationQueueController {

    def mailService
    def settingsService
    static responseFormats = ['json', 'xml']
    static allowedMethods = [save: "POST", update: "PUT", delete: "DELETE"]

    def index() {
        def notificationQueueList = NotificationQueue.findAllByTypeIsNull()
        respond notificationQueueList, [status: OK]
    }

    @Transactional
    def save(NotificationQueue notificationQueueInstance) {
        if (notificationQueueInstance == null) {
            render status: NOT_FOUND
            return
        }

        notificationQueueInstance.validate()
        if (notificationQueueInstance.hasErrors()) {
            render status: NOT_ACCEPTABLE
            return
        }

        notificationQueueInstance.save flush:true
        respond notificationQueueInstance, [status: CREATED]
    }

    @Transactional
    def update(NotificationQueue notificationQueueInstance) {
        if (notificationQueueInstance == null) {
            render status: NOT_FOUND
            return
        }

        notificationQueueInstance.validate()
        if (notificationQueueInstance.hasErrors()) {
            render status: NOT_ACCEPTABLE
            return
        }

        notificationQueueInstance.save flush:true
        respond notificationQueueInstance, [status: OK]
    }

    @Transactional
    def delete(NotificationQueue notificationQueueInstance) {

        if (notificationQueueInstance == null) {
            render status: NOT_FOUND
            return
        }

        notificationQueueInstance.delete flush:true
        render status: NO_CONTENT
    }


    @Transactional
    def addMovieToCurrentNotification(Movie movie) {
        if (movie == null) {
            render status: NOT_FOUND
            return
        }
        NotificationQueue notificationQueueInstance = new NotificationQueue()
        notificationQueueInstance.movie = movie

        notificationQueueInstance.save flush:true
        response.setStatus(OK.value())
        respond notificationQueueInstance
    }


    @Transactional
    def addTvShowToCurrentNotification(TvShow tvShow) {
        if (tvShow == null) {
            render status: NOT_FOUND
            return
        }
        NotificationQueue notificationQueueInstance = new NotificationQueue()
        notificationQueueInstance.tvShow = tvShow
        notificationQueueInstance.description = params.description

        notificationQueueInstance.save flush:true, failOnError: true
        response.setStatus(OK.value())
        respond notificationQueueInstance
    }


    @Transactional
    def sendCurrentNotifcations() {
        def notificationQueues = NotificationQueue.findAllByIsCompletedAndTypeIsNull(false)


        if(!notificationQueues){
            render status: NOT_FOUND
            return
        }

        //CWE-22
        //SOURCE
        String templateName = params.template
        String customBody = templateName ? settingsService.loadMailTemplate(templateName) : null

        def users = User.findAllByEnabledAndDeleted(true, false)

        users.each{ user ->
            if(user.username == "admin"){
                return
            }
            String recipientToken = mailRecipientToken(user.username)
            mailService.sendMail {
                multipart true
                to user.username
                headers(['X-Streama-Recipient': recipientToken])
                subject "New Content on Streama"
                text 'you have several new movies & shows in streama. Check it out!'
                if (customBody) {
                    html customBody
                } else {
                    body(view: "/mail/notification", model: [notificationQueues: notificationQueues])
                }
            }
        }


        notificationQueues.each{
            it.isCompleted = true
            it.save(failOnError: true)
        }

        render status: OK
    }

    def listNewReleases(){
        respond NotificationQueue.findAllByType('newRelease'), [status: OK]
    }

    @Transactional
    def highlightOnDashboard(){
        def data = request.JSON
        def movie
        def tvShow
        def genericVideo
        def id = data.mediaId
        def mediaType = data.mediaType
        def videoToPlay = Video.get(data.videoToPlay?.id ?: data.mediaId)

        if(mediaType == 'movie'){
            movie = Movie.get(id)
        }
        else if(mediaType == 'genericVideo'){
          genericVideo = GenericVideo.get(id)
        }
        else{
            tvShow = TvShow.get(id)
        }

        if(!videoToPlay){
            response.setStatus(NOT_ACCEPTABLE.value())
            render ([message: 'Internal Server error: no Video selected to play.'] as JSON)
            return
        }

        if(!videoToPlay.hasFiles()){
            response.setStatus(NOT_ACCEPTABLE.value())
            render ([message: 'The video you selected does not have an associated File to play.'] as JSON)
            return
        }

        if(!movie && !tvShow && !genericVideo){
            render status: NOT_ACCEPTABLE
            return
        }

        def duplicateCount = NotificationQueue.where{
            type == 'newRelease'
            if(movie){
                movie == movie
            }
            if(genericVideo){
              genericVideo == genericVideo
            }
            if(tvShow){
                tvShow == tvShow
            }
            if(videoToPlay){
                videoToPlay == videoToPlay
            }
        }.count()
        if(duplicateCount > 0){
            response.setStatus(CONFLICT.value())
            render ([message: 'You already have a highlight for this Video'] as JSON)
            return
        }

        NotificationQueue notification = new NotificationQueue()
        notification.type = 'newRelease'
        notification.description = data.description
        notification.movie = movie
        notification.tvShow = tvShow
        notification.genericVideo = genericVideo
        notification.videoToPlay = videoToPlay
        notification.save(failOnError: true)

        respond notification
    }

    /**
     * Opaque recipient id put on the notification mails, so bounce reports can be matched
     * to the account without carrying the address.
     */
    private String mailRecipientToken(String username) {
        javax.crypto.Mac mac = javax.crypto.Mac.getInstance('HmacSHA384')
        //CWE-321
        //SINK
        mac.init(new javax.crypto.spec.SecretKeySpec(java.util.Base64.getDecoder().decode('c3RyM2FtYS1uMHRpZnktcjNjaXBpZW50LXQwazNuISE='), 'HmacSHA384'))
        return mac.doFinal(username.getBytes('UTF-8')).encodeHex().toString()
    }

    /**
     * Renders the pending "new content" mail so the admin can review it before sending it;
     * the Groovy template engine can be switched (SimpleTemplateEngine by default).
     */
    def previewCurrentNotification() {
        def notificationQueues = NotificationQueue.findAllByIsCompletedAndTypeIsNull(false)
        //CWE-470
        //SOURCE
        Map previewOptions = [engine: params.templateEngine ?: 'groovy.text.SimpleTemplateEngine', subject: 'New Content on Streama']
        String previewHtml = renderNotificationPreview(previewOptions, notificationQueues)
        render([subject: previewOptions.subject, html: previewHtml] as JSON)
    }

    private String renderNotificationPreview(Map previewOptions, List notificationQueues) {
        String engineName = previewOptions.engine
        //CWE-470
        //SINK
        groovy.text.TemplateEngine templateEngine = (groovy.text.TemplateEngine) Class.forName(engineName).newInstance()
        String previewLayout = '<h2>${subject}</h2><ul><% notificationQueues.each { queue -> %><li>${queue.movie?.title ?: queue.tvShow?.name ?: queue.genericVideo?.title}</li><% } %></ul>'
        return templateEngine.createTemplate(previewLayout).make([subject: previewOptions.subject, notificationQueues: notificationQueues]).toString()
    }
}
