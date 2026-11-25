CWE-614
Example 1
Source/Sink:
[Application.groovy:31](/home/vitor/Desktop/DefensePoint/groovy-plant-issues/streama/grails-app/init/streama/Application.groovy#L31)


CWE-614
Example 2
Source/Sink:
[StreamaContainerSetup.groovy:27](/home/vitor/Desktop/DefensePoint/groovy-plant-issues/streama/src/main/groovy/streama/StreamaContainerSetup.groovy#L27)

Note: engineered host: ServletContextInitializer has no natural host in streama; added new class StreamaContainerSetup (cookie-only session tracking + configurable cookie maxAge) registered as a Spring bean in grails-app/conf/spring/resources.groovy


CWE-321
Example 1
Source/Sink:
[UserActivityService.groovy:55](/home/vitor/Desktop/DefensePoint/groovy-plant-issues/streama/grails-app/services/streama/UserActivityService.groovy#L55)


CWE-321
Example 2
Source/Sink:
[LoginController.groovy:177](/home/vitor/Desktop/DefensePoint/groovy-plant-issues/streama/grails-app/controllers/streama/LoginController.groovy#L177)


CWE-78
Example 1
Source:
[TranscodingController.groovy:351](/home/vitor/Desktop/DefensePoint/groovy-plant-issues/streama/grails-app/controllers/streama/TranscodingController.groovy#L351)

step 1:
[TranscodingController.groovy:356](/home/vitor/Desktop/DefensePoint/groovy-plant-issues/streama/grails-app/controllers/streama/TranscodingController.groovy#L356)

step 2:
[FfmpegService.groovy:36](/home/vitor/Desktop/DefensePoint/groovy-plant-issues/streama/grails-app/services/streama/FfmpegService.groovy#L36)

step 3:
[FfmpegService.groovy:44](/home/vitor/Desktop/DefensePoint/groovy-plant-issues/streama/grails-app/services/streama/FfmpegService.groovy#L44)

step 4:
[FfmpegService.groovy:50](/home/vitor/Desktop/DefensePoint/groovy-plant-issues/streama/grails-app/services/streama/FfmpegService.groovy#L50)

step 5:
[FfmpegService.groovy:510](/home/vitor/Desktop/DefensePoint/groovy-plant-issues/streama/grails-app/services/streama/FfmpegService.groovy#L510)

step 6:
[FfmpegService.groovy:511](/home/vitor/Desktop/DefensePoint/groovy-plant-issues/streama/grails-app/services/streama/FfmpegService.groovy#L511)

Sink:
[FfmpegService.groovy:514](/home/vitor/Desktop/DefensePoint/groovy-plant-issues/streama/grails-app/services/streama/FfmpegService.groovy#L514)


CWE-78
Example 2
Source:
[VideoController.groovy:280](/home/vitor/Desktop/DefensePoint/groovy-plant-issues/streama/grails-app/controllers/streama/VideoController.groovy#L280)

step 1:
[VideoController.groovy:281](/home/vitor/Desktop/DefensePoint/groovy-plant-issues/streama/grails-app/controllers/streama/VideoController.groovy#L281)

step 2:
[VideoService.groovy:238](/home/vitor/Desktop/DefensePoint/groovy-plant-issues/streama/grails-app/services/streama/VideoService.groovy#L238)

step 3:
[VideoService.groovy:240](/home/vitor/Desktop/DefensePoint/groovy-plant-issues/streama/grails-app/services/streama/VideoService.groovy#L240)

step 4:
[VideoConverterService.groovy:10](/home/vitor/Desktop/DefensePoint/groovy-plant-issues/streama/grails-app/services/streama/VideoConverterService.groovy#L10)

step 5:
[VideoConverterService.groovy:12](/home/vitor/Desktop/DefensePoint/groovy-plant-issues/streama/grails-app/services/streama/VideoConverterService.groovy#L12)

Sink:
[VideoConverterService.groovy:15](/home/vitor/Desktop/DefensePoint/groovy-plant-issues/streama/grails-app/services/streama/VideoConverterService.groovy#L15)


CWE-22
Example 1
Source:
[PlayerController.groovy:21](/home/vitor/Desktop/DefensePoint/groovy-plant-issues/streama/grails-app/controllers/streama/api/v1/PlayerController.groovy#L21)

step 1:
[PlayerController.groovy:23](/home/vitor/Desktop/DefensePoint/groovy-plant-issues/streama/grails-app/controllers/streama/api/v1/PlayerController.groovy#L23)

step 2:
[PlayerService.groovy:58](/home/vitor/Desktop/DefensePoint/groovy-plant-issues/streama/grails-app/services/streama/PlayerService.groovy#L58)

step 3:
[PlayerService.groovy:63](/home/vitor/Desktop/DefensePoint/groovy-plant-issues/streama/grails-app/services/streama/PlayerService.groovy#L63)

step 4:
[VideoHelper.groovy:17](/home/vitor/Desktop/DefensePoint/groovy-plant-issues/streama/src/main/groovy/streama/VideoHelper.groovy#L17)

step 5:
[VideoHelper.groovy:20](/home/vitor/Desktop/DefensePoint/groovy-plant-issues/streama/src/main/groovy/streama/VideoHelper.groovy#L20)

Sink:
[PlayerService.groovy:66](/home/vitor/Desktop/DefensePoint/groovy-plant-issues/streama/grails-app/services/streama/PlayerService.groovy#L66)


CWE-22
Example 2
Source:
[NotificationQueueController.groovy:111](/home/vitor/Desktop/DefensePoint/groovy-plant-issues/streama/grails-app/controllers/streama/NotificationQueueController.groovy#L111)

step 1:
[NotificationQueueController.groovy:112](/home/vitor/Desktop/DefensePoint/groovy-plant-issues/streama/grails-app/controllers/streama/NotificationQueueController.groovy#L112)

step 2:
[SettingsService.groovy:220](/home/vitor/Desktop/DefensePoint/groovy-plant-issues/streama/grails-app/services/streama/SettingsService.groovy#L220)

step 3:
[SettingsService.groovy:221](/home/vitor/Desktop/DefensePoint/groovy-plant-issues/streama/grails-app/services/streama/SettingsService.groovy#L221)

step 4:
[SettingsService.groovy:222](/home/vitor/Desktop/DefensePoint/groovy-plant-issues/streama/grails-app/services/streama/SettingsService.groovy#L222)

step 5:
[SettingsService.groovy:234](/home/vitor/Desktop/DefensePoint/groovy-plant-issues/streama/grails-app/services/streama/SettingsService.groovy#L234)

Sink:
[SettingsService.groovy:226](/home/vitor/Desktop/DefensePoint/groovy-plant-issues/streama/grails-app/services/streama/SettingsService.groovy#L226)


CWE-470
Example 2
Source:
[SettingsController.groovy:68](/home/vitor/Desktop/DefensePoint/groovy-plant-issues/streama/grails-app/controllers/streama/SettingsController.groovy#L68)

step 1:
[SettingsController.groovy:73](/home/vitor/Desktop/DefensePoint/groovy-plant-issues/streama/grails-app/controllers/streama/SettingsController.groovy#L73)

step 2:
[SettingsService.groovy:245](/home/vitor/Desktop/DefensePoint/groovy-plant-issues/streama/grails-app/services/streama/SettingsService.groovy#L245)

step 3:
[SettingsService.groovy:253](/home/vitor/Desktop/DefensePoint/groovy-plant-issues/streama/grails-app/services/streama/SettingsService.groovy#L253)

Sink:
[SettingsService.groovy:256](/home/vitor/Desktop/DefensePoint/groovy-plant-issues/streama/grails-app/services/streama/SettingsService.groovy#L256)


CWE-470
Example 1
Source:
[ReportController.groovy:49](/home/vitor/Desktop/DefensePoint/groovy-plant-issues/streama/grails-app/controllers/streama/ReportController.groovy#L49)

step 1:
[ReportController.groovy:50](/home/vitor/Desktop/DefensePoint/groovy-plant-issues/streama/grails-app/controllers/streama/ReportController.groovy#L50)

step 2:
[ReportController.groovy:51](/home/vitor/Desktop/DefensePoint/groovy-plant-issues/streama/grails-app/controllers/streama/ReportController.groovy#L51)

step 3:
[MediaService.groovy:33](/home/vitor/Desktop/DefensePoint/groovy-plant-issues/streama/grails-app/services/streama/MediaService.groovy#L33)

step 4:
[MediaService.groovy:35](/home/vitor/Desktop/DefensePoint/groovy-plant-issues/streama/grails-app/services/streama/MediaService.groovy#L35)

step 5:
[MediaService.groovy:43](/home/vitor/Desktop/DefensePoint/groovy-plant-issues/streama/grails-app/services/streama/MediaService.groovy#L43)

Sink:
[MediaService.groovy:49](/home/vitor/Desktop/DefensePoint/groovy-plant-issues/streama/grails-app/services/streama/MediaService.groovy#L49)


CWE-88
Example 2
Source:
[FileController.groovy:443](/home/vitor/Desktop/DefensePoint/groovy-plant-issues/streama/grails-app/controllers/streama/FileController.groovy#L443)

step 1:
[FileController.groovy:445](/home/vitor/Desktop/DefensePoint/groovy-plant-issues/streama/grails-app/controllers/streama/FileController.groovy#L445)

step 2:
[MediaService.groovy:58](/home/vitor/Desktop/DefensePoint/groovy-plant-issues/streama/grails-app/services/streama/MediaService.groovy#L58)

step 3:
[MediaService.groovy:60](/home/vitor/Desktop/DefensePoint/groovy-plant-issues/streama/grails-app/services/streama/MediaService.groovy#L60)

Sink:
[MediaService.groovy:64](/home/vitor/Desktop/DefensePoint/groovy-plant-issues/streama/grails-app/services/streama/MediaService.groovy#L64)


CWE-400
Example 2
Source:
[FileController.groovy:526](/home/vitor/Desktop/DefensePoint/groovy-plant-issues/streama/grails-app/controllers/streama/FileController.groovy#L526)

step 1:
[FileController.groovy:529](/home/vitor/Desktop/DefensePoint/groovy-plant-issues/streama/grails-app/controllers/streama/FileController.groovy#L529)

step 2:
[TranscodingService.groovy:470](/home/vitor/Desktop/DefensePoint/groovy-plant-issues/streama/grails-app/services/streama/TranscodingService.groovy#L470)

step 3:
[TranscodingService.groovy:471](/home/vitor/Desktop/DefensePoint/groovy-plant-issues/streama/grails-app/services/streama/TranscodingService.groovy#L471)

step 4:
[TranscodingService.groovy:475](/home/vitor/Desktop/DefensePoint/groovy-plant-issues/streama/grails-app/services/streama/TranscodingService.groovy#L475)

Sink:
[TranscodingService.groovy:479](/home/vitor/Desktop/DefensePoint/groovy-plant-issues/streama/grails-app/services/streama/TranscodingService.groovy#L479)


CWE-400
Example 1
Source:
[TheMovieDbController.groovy:150](/home/vitor/Desktop/DefensePoint/groovy-plant-issues/streama/grails-app/controllers/streama/TheMovieDbController.groovy#L150)

step 1:
[TheMovieDbController.groovy:179](/home/vitor/Desktop/DefensePoint/groovy-plant-issues/streama/grails-app/controllers/streama/TheMovieDbController.groovy#L179)

step 2:
[TheMovieDbService.groovy:296](/home/vitor/Desktop/DefensePoint/groovy-plant-issues/streama/grails-app/services/streama/TheMovieDbService.groovy#L296)

step 3:
[TheMovieDbService.groovy:297](/home/vitor/Desktop/DefensePoint/groovy-plant-issues/streama/grails-app/services/streama/TheMovieDbService.groovy#L297)

step 4:
[TheMovieDbService.groovy:321](/home/vitor/Desktop/DefensePoint/groovy-plant-issues/streama/grails-app/services/streama/TheMovieDbService.groovy#L321)

Sink:
[TheMovieDbService.groovy:325](/home/vitor/Desktop/DefensePoint/groovy-plant-issues/streama/grails-app/services/streama/TheMovieDbService.groovy#L325)


CWE-88
Example 1
Source:
[MovieController.groovy:58](/home/vitor/Desktop/DefensePoint/groovy-plant-issues/streama/grails-app/controllers/streama/MovieController.groovy#L58)

step 1:
[MovieController.groovy:60](/home/vitor/Desktop/DefensePoint/groovy-plant-issues/streama/grails-app/controllers/streama/MovieController.groovy#L60)

step 2:
[TheMovieDbService.groovy:348](/home/vitor/Desktop/DefensePoint/groovy-plant-issues/streama/grails-app/services/streama/TheMovieDbService.groovy#L348)

step 3:
[TheMovieDbService.groovy:354](/home/vitor/Desktop/DefensePoint/groovy-plant-issues/streama/grails-app/services/streama/TheMovieDbService.groovy#L354)

Sink:
[TheMovieDbService.groovy:358](/home/vitor/Desktop/DefensePoint/groovy-plant-issues/streama/grails-app/services/streama/TheMovieDbService.groovy#L358)


CWE-400
Example 3
Source:
[WebsocketController.groovy:18](/home/vitor/Desktop/DefensePoint/groovy-plant-issues/streama/grails-app/controllers/streama/WebsocketController.groovy#L18)

step 1:
[WebsocketController.groovy:20](/home/vitor/Desktop/DefensePoint/groovy-plant-issues/streama/grails-app/controllers/streama/WebsocketController.groovy#L20)

step 2:
[WebsocketController.groovy:23](/home/vitor/Desktop/DefensePoint/groovy-plant-issues/streama/grails-app/controllers/streama/WebsocketController.groovy#L23)

step 3:
[WebsocketController.groovy:29](/home/vitor/Desktop/DefensePoint/groovy-plant-issues/streama/grails-app/controllers/streama/WebsocketController.groovy#L29)

Sink:
[WebsocketController.groovy:32](/home/vitor/Desktop/DefensePoint/groovy-plant-issues/streama/grails-app/controllers/streama/WebsocketController.groovy#L32)


CWE-400
Example 4
Source:
[SubtitlesController.groovy:75](/home/vitor/Desktop/DefensePoint/groovy-plant-issues/streama/grails-app/controllers/streama/SubtitlesController.groovy#L75)

step 1:
[SubtitlesController.groovy:78](/home/vitor/Desktop/DefensePoint/groovy-plant-issues/streama/grails-app/controllers/streama/SubtitlesController.groovy#L78)

step 2:
[OpensubtitlesService.groovy:142](/home/vitor/Desktop/DefensePoint/groovy-plant-issues/streama/grails-app/services/streama/OpensubtitlesService.groovy#L142)

step 3:
[OpensubtitlesService.groovy:156](/home/vitor/Desktop/DefensePoint/groovy-plant-issues/streama/grails-app/services/streama/OpensubtitlesService.groovy#L156)

step 4:
[OpensubtitlesService.groovy:238](/home/vitor/Desktop/DefensePoint/groovy-plant-issues/streama/grails-app/services/streama/OpensubtitlesService.groovy#L238)

step 5:
[OpensubtitlesService.groovy:240](/home/vitor/Desktop/DefensePoint/groovy-plant-issues/streama/grails-app/services/streama/OpensubtitlesService.groovy#L240)

step 6:
[OpensubtitlesService.groovy:424](/home/vitor/Desktop/DefensePoint/groovy-plant-issues/streama/grails-app/services/streama/OpensubtitlesService.groovy#L424)

Sink:
[OpensubtitlesService.groovy:427](/home/vitor/Desktop/DefensePoint/groovy-plant-issues/streama/grails-app/services/streama/OpensubtitlesService.groovy#L427)


CWE-400
Example 5
Source:
[ViewingStatusController.groovy:120](/home/vitor/Desktop/DefensePoint/groovy-plant-issues/streama/grails-app/controllers/streama/ViewingStatusController.groovy#L120)

step 1:
[ViewingStatusController.groovy:121](/home/vitor/Desktop/DefensePoint/groovy-plant-issues/streama/grails-app/controllers/streama/ViewingStatusController.groovy#L121)

step 2:
[ViewingStatusController.groovy:122](/home/vitor/Desktop/DefensePoint/groovy-plant-issues/streama/grails-app/controllers/streama/ViewingStatusController.groovy#L122)

step 3:
[ViewingStatusService.groovy:97](/home/vitor/Desktop/DefensePoint/groovy-plant-issues/streama/grails-app/services/streama/ViewingStatusService.groovy#L97)

step 4:
[ViewingStatusService.groovy:98](/home/vitor/Desktop/DefensePoint/groovy-plant-issues/streama/grails-app/services/streama/ViewingStatusService.groovy#L98)

step 5:
[ViewingStatusService.groovy:113](/home/vitor/Desktop/DefensePoint/groovy-plant-issues/streama/grails-app/services/streama/ViewingStatusService.groovy#L113)

Sink:
[ViewingStatusService.groovy:102](/home/vitor/Desktop/DefensePoint/groovy-plant-issues/streama/grails-app/services/streama/ViewingStatusService.groovy#L102)


CWE-614
Example 3
Source/Sink:
[LegacyPlayerSessionInitializer.groovy:19](/home/vitor/Desktop/DefensePoint/groovy-plant-issues/streama/src/main/groovy/streama/LegacyPlayerSessionInitializer.groovy#L19)

Note: engineered host: engineered host: streama has no startup-time servlet configuration component for player compatibility; LegacyPlayerSessionInitializer (Spring Boot ServletContextInitializer) was created and declared as a bean in Application.doWithSpring().


CWE-614
Example 4
Source/Sink:
[CastReceiverContainerCustomizer.groovy:25](/home/vitor/Desktop/DefensePoint/groovy-plant-issues/streama/src/main/groovy/streama/CastReceiverContainerCustomizer.groovy#L25)

Note: engineered host: engineered host: streama has no embedded-container customization for cast receivers; CastReceiverContainerCustomizer (Spring Boot EmbeddedServletContainerCustomizer adding a ServletContextInitializer) was created and declared in grails-app/conf/spring/resources.groovy.


CWE-614
Example 5
Source/Sink:
[MediaServerModeInitializer.groovy:35](/home/vitor/Desktop/DefensePoint/groovy-plant-issues/streama/src/main/groovy/streama/MediaServerModeInitializer.groovy#L35)

Note: engineered host: engineered host: streama has no Tomcat-level startup hook for the LAN media-server mode; MediaServerModeInitializer (EmbeddedServletContainerCustomizer + TomcatContextCustomizer registering itself as a javax.servlet.ServletContainerInitializer) was created and declared in resources.groovy.


CWE-614
Example 6
Source/Sink:
[RemoteAccessSessionListener.groovy:23](/home/vitor/Desktop/DefensePoint/groovy-plant-issues/streama/src/main/groovy/streama/RemoteAccessSessionListener.groovy#L23)

Note: engineered host: engineered host: streama has no servlet listener for remote (reverse-proxy) access; RemoteAccessSessionListener (ServletContextListener) plus RemoteAccessTomcatCustomizer (declares it with Context.addApplicationListener) were created in one new file and the customizer was declared in resources.groovy.


CWE-614
Example 7
Source/Sink:
[TvClientTomcatSupport.groovy:30](/home/vitor/Desktop/DefensePoint/groovy-plant-issues/streama/src/main/groovy/streama/TvClientTomcatSupport.groovy#L30)

Note: engineered host: engineered host: streama has no Tomcat lifecycle hook for TV clients; TvClientTomcatSupport (EmbeddedServletContainerCustomizer + Tomcat LifecycleListener handling configure_start) was created and declared in resources.groovy.


CWE-614
Example 8
Source/Sink:
[Application.groovy:68](/home/vitor/Desktop/DefensePoint/groovy-plant-issues/streama/grails-app/init/streama/Application.groovy#L68)


CWE-614
Example 9
Source/Sink:
[StompSessionConfiguration.groovy:27](/home/vitor/Desktop/DefensePoint/groovy-plant-issues/streama/src/main/groovy/streama/StompSessionConfiguration.groovy#L27)

Note: engineered host: engineered host: streama has no Spring configuration for its STOMP/SockJS sessions; StompSessionConfiguration (@Configuration with a ServletContextInitializer @Bean) was created and imported with @Import on Application.


CWE-614
Example 10
Source/Sink:
[MediaResponseHeadersFilter.groovy:21](/home/vitor/Desktop/DefensePoint/groovy-plant-issues/streama/src/main/groovy/streama/MediaResponseHeadersFilter.groovy#L21)

Note: engineered host: engineered host: streama has no servlet filter for external-player responses; MediaResponseHeadersFilter (javax.servlet.Filter whose init configures the session cookie) was created and declared as a bean in resources.groovy.


CWE-321
Example 3
Source/Sink:
[WebsocketController.groovy:47](/home/vitor/Desktop/DefensePoint/groovy-plant-issues/streama/grails-app/controllers/streama/WebsocketController.groovy#L47)


CWE-321
Example 4
Source/Sink:
[ApiController.groovy:72](/home/vitor/Desktop/DefensePoint/groovy-plant-issues/streama/grails-app/controllers/streama/api/v1/ApiController.groovy#L72)


CWE-321
Example 5
Source/Sink:
[WatchlistEntryController.groovy:187](/home/vitor/Desktop/DefensePoint/groovy-plant-issues/streama/grails-app/controllers/streama/WatchlistEntryController.groovy#L187)


CWE-321
Example 6
Source/Sink:
[NotificationQueueController.groovy:226](/home/vitor/Desktop/DefensePoint/groovy-plant-issues/streama/grails-app/controllers/streama/NotificationQueueController.groovy#L226)


CWE-321
Example 7
Source/Sink:
[UserActivityController.groovy:96](/home/vitor/Desktop/DefensePoint/groovy-plant-issues/streama/grails-app/controllers/streama/UserActivityController.groovy#L96)


CWE-321
Example 8
Source/Sink:
[PlayerService.groovy:83](/home/vitor/Desktop/DefensePoint/groovy-plant-issues/streama/grails-app/services/streama/PlayerService.groovy#L83)


CWE-321
Example 9
Source/Sink:
[TranscodingService.groovy:492](/home/vitor/Desktop/DefensePoint/groovy-plant-issues/streama/grails-app/services/streama/TranscodingService.groovy#L492)


CWE-321
Example 10
Source/Sink:
[SubtitlesController.groovy:121](/home/vitor/Desktop/DefensePoint/groovy-plant-issues/streama/grails-app/controllers/streama/SubtitlesController.groovy#L121)


CWE-470
Example 3
Source:
[EpisodeController.groovy:21](/home/vitor/Desktop/DefensePoint/groovy-plant-issues/streama/grails-app/controllers/streama/EpisodeController.groovy#L21)

step 1:
[EpisodeController.groovy:23](/home/vitor/Desktop/DefensePoint/groovy-plant-issues/streama/grails-app/controllers/streama/EpisodeController.groovy#L23)

step 2:
[VideoService.groovy:247](/home/vitor/Desktop/DefensePoint/groovy-plant-issues/streama/grails-app/services/streama/VideoService.groovy#L247)

step 3:
[VideoService.groovy:248](/home/vitor/Desktop/DefensePoint/groovy-plant-issues/streama/grails-app/services/streama/VideoService.groovy#L248)

step 4:
[VideoService.groovy:252](/home/vitor/Desktop/DefensePoint/groovy-plant-issues/streama/grails-app/services/streama/VideoService.groovy#L252)

Sink:
[VideoService.groovy:255](/home/vitor/Desktop/DefensePoint/groovy-plant-issues/streama/grails-app/services/streama/VideoService.groovy#L255)


CWE-470
Example 4
Source:
[BulkController.groovy:12](/home/vitor/Desktop/DefensePoint/groovy-plant-issues/streama/grails-app/controllers/streama/BulkController.groovy#L12)

step 1:
[BulkController.groovy:13](/home/vitor/Desktop/DefensePoint/groovy-plant-issues/streama/grails-app/controllers/streama/BulkController.groovy#L13)

step 2:
[BulkCreateService.groovy:332](/home/vitor/Desktop/DefensePoint/groovy-plant-issues/streama/grails-app/services/streama/BulkCreateService.groovy#L332)

step 3:
[BulkCreateService.groovy:333](/home/vitor/Desktop/DefensePoint/groovy-plant-issues/streama/grails-app/services/streama/BulkCreateService.groovy#L333)

Sink:
[BulkCreateService.groovy:338](/home/vitor/Desktop/DefensePoint/groovy-plant-issues/streama/grails-app/services/streama/BulkCreateService.groovy#L338)


CWE-470
Example 5
Source:
[NotificationQueueController.groovy:238](/home/vitor/Desktop/DefensePoint/groovy-plant-issues/streama/grails-app/controllers/streama/NotificationQueueController.groovy#L238)

step 1:
[NotificationQueueController.groovy:239](/home/vitor/Desktop/DefensePoint/groovy-plant-issues/streama/grails-app/controllers/streama/NotificationQueueController.groovy#L239)

step 2:
[NotificationQueueController.groovy:243](/home/vitor/Desktop/DefensePoint/groovy-plant-issues/streama/grails-app/controllers/streama/NotificationQueueController.groovy#L243)

step 3:
[NotificationQueueController.groovy:244](/home/vitor/Desktop/DefensePoint/groovy-plant-issues/streama/grails-app/controllers/streama/NotificationQueueController.groovy#L244)

Sink:
[NotificationQueueController.groovy:247](/home/vitor/Desktop/DefensePoint/groovy-plant-issues/streama/grails-app/controllers/streama/NotificationQueueController.groovy#L247)


CWE-470
Example 6
Source:
[GenericVideoController.groovy:19](/home/vitor/Desktop/DefensePoint/groovy-plant-issues/streama/grails-app/controllers/streama/GenericVideoController.groovy#L19)

step 1:
[GenericVideoController.groovy:21](/home/vitor/Desktop/DefensePoint/groovy-plant-issues/streama/grails-app/controllers/streama/GenericVideoController.groovy#L21)

step 2:
[VideoHelper.groovy:23](/home/vitor/Desktop/DefensePoint/groovy-plant-issues/streama/src/main/groovy/streama/VideoHelper.groovy#L23)

Sink:
[VideoHelper.groovy:26](/home/vitor/Desktop/DefensePoint/groovy-plant-issues/streama/src/main/groovy/streama/VideoHelper.groovy#L26)


CWE-470
Example 7
Source:
[UserActivityController.groovy:111](/home/vitor/Desktop/DefensePoint/groovy-plant-issues/streama/grails-app/controllers/streama/UserActivityController.groovy#L111)

step 1:
[UserActivityController.groovy:112](/home/vitor/Desktop/DefensePoint/groovy-plant-issues/streama/grails-app/controllers/streama/UserActivityController.groovy#L112)

step 2:
[UserActivityService.groovy:62](/home/vitor/Desktop/DefensePoint/groovy-plant-issues/streama/grails-app/services/streama/UserActivityService.groovy#L62)

step 3:
[UserActivityService.groovy:63](/home/vitor/Desktop/DefensePoint/groovy-plant-issues/streama/grails-app/services/streama/UserActivityService.groovy#L63)

Sink:
[UserActivityService.groovy:68](/home/vitor/Desktop/DefensePoint/groovy-plant-issues/streama/grails-app/services/streama/UserActivityService.groovy#L68)


CWE-470
Example 8
Source:
[DashController.groovy:301](/home/vitor/Desktop/DefensePoint/groovy-plant-issues/streama/grails-app/controllers/streama/DashController.groovy#L301)

step 1:
[DashController.groovy:308](/home/vitor/Desktop/DefensePoint/groovy-plant-issues/streama/grails-app/controllers/streama/DashController.groovy#L308)

Sink:
[DashController.groovy:311](/home/vitor/Desktop/DefensePoint/groovy-plant-issues/streama/grails-app/controllers/streama/DashController.groovy#L311)
