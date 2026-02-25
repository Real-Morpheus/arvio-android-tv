package com.arflix.tv.data.model

enum class CatalogSourceType {
    TMDB,
    STREMIO,
    CLOUDSTREAM
}

data class CatalogConfig(
    val id: String,
    val name: String,
    val sourceType: CatalogSourceType,
    val addonUrl: String? = null,
    val catalogType: String? = null,
    val catalogId: String? = null,
    val enabled: Boolean = true
)

data class MediaItem(
    val id: String,
    val title: String,
    val posterUrl: String? = null,
    val backdropUrl: String? = null,
    val overview: String? = null,
    val releaseDate: String? = null,
    val mediaType: MediaType = MediaType.MOVIE,
    val rating: Double? = null,
    val source: CatalogSourceType = CatalogSourceType.TMDB,
    val sourceLabel: String? = null,
    val imdbId: String? = null,
    val year: Int? = null,
    val genres: List<String>? = null
)

enum class MediaType {
    MOVIE,
    TV
}

data class SearchResults(
    val movies: List<MediaItem> = emptyList(),
    val tvShows: List<MediaItem> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null
)
