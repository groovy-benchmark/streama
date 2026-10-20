package streama

class InviteController {

    def mailService
    def grailsApplication

    def index(){
    }

    def setPassword() {
        def password = params.password        
        def passwordRepeat = params.passwordRepeat
        String uuid = params.uuid

        User user = User.findByUuid(uuid)
        
        if(!uuid || !user || !password || !passwordRepeat || password != passwordRepeat || password.size() <= 5){
            redirect(action: 'index')
            return
        }

        user.password = password
        user.uuid = null
        user.save flush: true, failOnError: true

        redirect(uri: '/')
    }

    /**
     * POST /invite/requestInvite - A visitor asks for an invitation; the confirmation is mailed to them
     */
    def requestInvite() {
        Map msg = [
            to     : params.email,
            subject: "Your Streama invitation request",
            text   : "We received your invitation request and will get back to you."
        ]
        deliver(msg)
        render status: 202
    }

    /**
     * POST /invite/requestInviteStatus - Mail the status of a request to the registered account
     */
    def requestInviteStatus() {
        Map msg = [
            username: params.email,
            subject : "Your Streama invitation status",
            text    : "Your invitation request is being processed."
        ]
        deliverToAccount(msg)
        render status: 202
    }

    /**
     * POST /invite/shareInvite - A visitor sends a note to the site team, shown as coming from the visitor
     */
    def shareInvite() {
        sendShareMail(params.senderEmail, "Note from a visitor")
        render status: 202
    }

    /**
     * POST /invite/shareInviteSystem - Same note, sent from the application's own address
     */
    def shareInviteSystem() {
        sendShareMailAs(params.senderEmail, "Note from a visitor")
        render status: 202
    }

    /**
     * POST /invite/copyInvite - Mail a reminder to the account and hidden copies to the addresses given
     */
    def copyInvite() {
        User user = User.findByUsername(params.email)
        if (!user) {
            render status: 404
            return
        }
        List<String> copies = (params.copyTo ?: '').tokenize(',')*.trim()
        mailService.sendMail {
            to user.username
            bcc copies // SINK: PLANTED-GR-HR-20
            subject "Your Streama invitation"
            text "Your invitation is waiting."
        }
        render status: 202
    }

    /**
     * POST /invite/copyInviteFixed - Same reminder, copies go to the configured archive address only
     */
    def copyInviteFixed() {
        User user = User.findByUsername(params.email)
        if (!user) {
            render status: 404
            return
        }
        String archive = grailsApplication.config.streama.invite.archive ?: 'archive@streama.local'
        mailService.sendMail {
            to user.username
            bcc archive // SAFE_SINK: PLANTED-GR-HR-20-safe
            subject "Your Streama invitation"
            text "Your invitation is waiting."
        }
        render status: 202
    }

    private void deliver(Map msg) {
        mailService.sendMail {
            to msg.to // SINK: PLANTED-GR-HR-17
            subject msg.subject
            text msg.text
        }
    }

    private void deliverToAccount(Map msg) {
        User account = User.findByUsername(msg.username)
        if (!account) {
            return
        }
        mailService.sendMail {
            to account.username // SAFE_SINK: PLANTED-GR-HR-17-safe
            subject msg.subject
            text msg.text
        }
    }

    private void sendShareMail(String sender, String subjectLine) {
        mailService.sendMail {
            to grailsApplication.config.streama.invite.archive ?: 'archive@streama.local'
            from sender // SINK: PLANTED-GR-HR-18
            subject subjectLine
            text "A visitor left a note for the team."
        }
    }

    private void sendShareMailAs(String sender, String subjectLine) {
        String fixedFrom = grailsApplication.config.grails.mail.default.from ?: 'noreply@streama.local'
        mailService.sendMail {
            to grailsApplication.config.streama.invite.archive ?: 'archive@streama.local'
            from fixedFrom // SAFE_SINK: PLANTED-GR-HR-18-safe
            subject subjectLine
            text "Sent by ${sender}"
        }
    }
}
