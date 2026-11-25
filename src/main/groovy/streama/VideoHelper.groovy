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
}
