package streama

import grails.transaction.Transactional
import grails.web.servlet.mvc.GrailsParameterMap

import static org.springframework.http.HttpStatus.CREATED
import static org.springframework.http.HttpStatus.NOT_ACCEPTABLE

@Transactional
class PlayerService {

    def springSecurityService
    def uploadService

    @Transactional
    def updateViewingStatus(GrailsParameterMap params) {
        User currentUser = springSecurityService.currentUser
        Video video = Video.get(params.getInt('videoId'))
        Double currentTime = params.getDouble('currentTime');
        Double runtime = params.getDouble('runtime');
        ViewingStatus viewingStatus

        if (!video || currentTime == null) {
            return ResultHelper.generateErrorResult(NOT_ACCEPTABLE, 'video_currentTime', 'video or currentTime missing')
        }

        if(video instanceof Episode){
            viewingStatus = ViewingStatus.findOrCreateByTvShowAndUser(video.show, currentUser)
            viewingStatus.tvShow = video.show
        }else{
            viewingStatus = ViewingStatus.findOrCreateByVideoAndUser(video, currentUser)
        }

        viewingStatus.video = video
        viewingStatus.currentPlayTime = currentTime
        viewingStatus.runtime = runtime
        viewingStatus.user = currentUser
        viewingStatus.profile = params.profile


        viewingStatus.validate()
        if (viewingStatus.hasErrors()) {
            return ResultHelper.generateErrorResult(NOT_ACCEPTABLE, 'viewingStatus', 'viewingStatus validate')
        }

        viewingStatus.save flush:true
        return ResultHelper.generateOkResult(CREATED, viewingStatus)
    }

    /**
     * Reads a sidecar subtitle (e.g. "movie.en.srt") stored next to the video's local file.
     *
     * @param video the video being played
     * @param sidecarName file name of the sidecar track
     * @return the raw subtitle text
     */
    @Transactional(readOnly = true)
    String loadSidecarSubtitle(Video video, String sidecarName) {
        String videoPath = video.getDefaultVideoFile()?.localFile ?: uploadService.localPath
        if (!videoPath) {
            return ''
        }
        String sidecarPath = VideoHelper.sidecarPath(videoPath, sidecarName.replace('..\\', ''))
        //CWE-22
        //SINK
        return new java.io.File(sidecarPath).text
    }

    private static final byte[] PLAYBACK_SESSION_KEY = 'pl4yb4ck-s3ss10n-k3y'.getBytes('UTF-8')

    /**
     * Signed playback session handed to the player together with the video, so the
     * viewing-status updates it sends back can be tied to the video that was opened.
     *
     * @param video the video being played
     * @return "<userId>:<videoId>:<openedAt>.<signature>"
     */
    String playbackSessionToken(Video video) {
        String session = "${springSecurityService.currentUserId}:${video.id}:${System.currentTimeMillis()}".toString()
        javax.crypto.Mac mac = javax.crypto.Mac.getInstance('HmacSHA256')
        //CWE-321
        //SINK
        mac.init(new javax.crypto.spec.SecretKeySpec(PLAYBACK_SESSION_KEY, 'HmacSHA256'))
        return session + '.' + mac.doFinal(session.getBytes('UTF-8')).encodeHex().toString()
    }

}
