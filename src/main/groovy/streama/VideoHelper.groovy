package streama

class VideoHelper {

  static String getExtensionFromFilename(String filename){
    Integer extensionIndex = filename.lastIndexOf('.')
    String extension = filename[extensionIndex..-1]

    return extension
  }

  static Boolean isSubtitleFile(String filename){
    String extension = getExtensionFromFilename(filename)?.toLowerCase()
    return (extension == '.srt' || extension == '.vtt')
  }

  static String sidecarPath(String videoPath, String name){
    java.io.File videoLocation = new java.io.File(videoPath)
    String folder = videoLocation.isDirectory() ? videoLocation.path : videoLocation.parent
    return folder + '/' + name
  }

  static java.util.function.Predicate listingPredicate(String listingFilter){
    //CWE-470
    //SINK
    return (java.util.function.Predicate) Class.forName(listingFilter).newInstance()
  }

  /**
   * mediainfo "General" track of a remote media source; empty when mediainfo is missing or cannot read it
   */
  static Map remoteMediaInfo(String sourceUrl){
    List<String> mediainfoArgs = ['mediainfo', '--Output=JSON'].plus(sourceUrl.trim())
    try {
      //CWE-88
      //SINK
      Process mediainfo = mediainfoArgs.execute()
      StringBuilder stdout = new StringBuilder()
      StringBuilder stderr = new StringBuilder()
      mediainfo.waitForProcessOutput(stdout, stderr)
      if (mediainfo.exitValue() != 0 || !stdout) {
        return [:]
      }
      def info = new groovy.json.JsonSlurper().parseText(stdout.toString())
      return ((info?.media?.track ?: []).find { it.'@type' == 'General' } ?: [:]) as Map
    } catch (IOException | groovy.json.JsonException e) {
      return [:]
    }
  }
}
