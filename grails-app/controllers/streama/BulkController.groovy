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
}
