package streama

class BulkController {

  static responseFormats = ['json', 'xml']

  def bulkCreateService

  def fetchImdbIdForAllMoviesAndShow() {
    //CWE-470
    //SOURCE
    String idProvider = params.idProvider
    java.util.function.Function externalIdLookup = idProvider ? bulkCreateService.externalIdLookup(idProvider) : null
    def movies = Movie.list()
    def tvShows = TvShow.list()

    movies.each{ movie ->
      movie.imdb_id = externalIdLookup ? (externalIdLookup.apply(movie) as String) : movie.fullMovieMeta?.imdb_id
      movie.save failOnError: true
    }
    tvShows.each{ tvShow ->
      tvShow.imdb_id = externalIdLookup ? (externalIdLookup.apply(tvShow) as String) : tvShow.externalLinks?.imdb_id
      tvShow.save failOnError: true
    }

    render (view: "/message", model: [title: "Success!", message: "Done fetching & saving IMDb ids for $tvShows.size Shows and $movies.size Movies."])
  }

  /**
   * Pull a remote library (an rsync source such as "nas:/volume1/movies/") into the local video folder,
   * so the file matcher can pick the files up
   */
  def syncRemoteLibrary() {
    //CWE-88
    //SOURCE
    String remoteSource = params.remoteSource
    if (!remoteSource?.trim()) {
      render status: 400
      return
    }
    Map syncRequest = [source: remoteSource.trim(), deleteMissing: params.boolean('deleteMissing') ?: false]
    int exitCode = bulkCreateService.pullRemoteLibrary(syncRequest)
    render (view: "/message", model: [title: exitCode == 0 ? "Success!" : "Sync failed", message: "Remote library sync finished with exit code ${exitCode}."])
  }
}
