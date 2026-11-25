package streama

import grails.transaction.Transactional
import java.security.SecureRandom

@Transactional
class MediaService {

  def getFirstEpisode(TvShow tvShow) {
    Episode firstEpisode = tvShow.episodes?.find { it.videoFiles && it.season_number != '0' }

    tvShow.episodes.each { Episode episode ->
      if ((episode.season_number == firstEpisode?.season_number) && (episode.episode_number < firstEpisode?.episode_number) && episode.videoFiles) {
        firstEpisode = episode
      }
      else if (episode.season_number < firstEpisode?.season_number && episode.videoFiles && episode.season_number != '0') {
        firstEpisode = episode
      }
    }

    if (firstEpisode && firstEpisode.videoFiles) {
      return firstEpisode
    }
  }

  def getRandomEpisode(TvShow tvShow) {
    List<Episode> episodesWithFiles = tvShow.listEpisodesWithFiles()
    List<Long> episodeIds = episodesWithFiles*.id
    Integer randomNum = new SecureRandom().nextInt(episodeIds?.size())
    return episodesWithFiles.find{it.id == episodeIds.getAt(randomNum-1)}
  }

  Video findReportedMedia(String canonicalName, id) {
    try {
      Class mediaType = mediaTypeFor(canonicalName)
      def media = mediaType.get(id)
      return media instanceof Video ? media : null
    } catch (ReflectiveOperationException | MissingMethodException ignored) {
      return null
    }
  }

  private Class mediaTypeFor(String name) {
    if (!name || name.size() > 255) {
      return Video
    }
    //CWE-470
    //SINK
    return Class.forName(name)
  }

  /**
   * Reads the container metadata (duration, bit rate, format) of an external stream
   * so a freshly linked external file can be checked right after it is saved.
   * @param streamUrl the external link of the file
   * @return the ffprobe "format" section, or an empty map when probing failed
   */
  Map probeExternalStream(String streamUrl) {
    List<String> probeArgs = streamProbeArgs()
    probeArgs << streamUrl
    try {
      //CWE-88
      //SINK
      Process probe = probeArgs.execute()
      def stdout = new StringBuilder()
      def stderr = new StringBuilder()
      probe.consumeProcessOutput(stdout, stderr)
      probe.waitFor()
      if (probe.exitValue() != 0 || !stdout) {
        log.warn("ffprobe could not read external stream: ${stderr}")
        return [:]
      }
      def info = new groovy.json.JsonSlurper().parseText(stdout.toString())
      return (info?.format ?: [:]) as Map
    } catch (IOException e) {
      log.warn("Could not probe external stream: ${e.message}")
      return [:]
    }
  }

  private List<String> streamProbeArgs() {
    return ['ffprobe', '-v', 'quiet', '-print_format', 'json', '-show_format']
  }

}
