package com.arflix.tv.data.model

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class TmdbMovieSearchResponse(
    val page: Int,
    val results: List<TmdbMovie>,
    @Json(name = "total_pages") val totalPages: Int,
    @Json(name = "total_results") val totalResults: Int
)

@JsonClass(generateAdapter = true)
data class TmdbMovie(
    val id: Int,
    val title: String,
    @Json(name = "poster_path") val posterPath: String? = null,
    @Json(name = "backdrop_path") val backdropPath: String? = null,
    val overview: String? = null,
    @Json(name = "release_date") val releaseDate: String? = null,
    @Json(name = "vote_average") val voteAverage: Double? = null,
    @Json(name = "genre_ids") val genreIds: List<Int>? = null,
    val adult: Boolean = false
)

@JsonClass(generateAdapter = true)
data class TmdbTvSearchResponse(
    val page: Int,
    val results: List<TmdbTv>,
    @Json(name = "total_pages") val totalPages: Int,
    @Json(name = "total_results") val totalResults: Int
)

@JsonClass(generateAdapter = true)
data class TmdbTv(
    val id: Int,
    val name: String,
    @Json(name = "poster_path") val posterPath: String? = null,
    @Json(name = "backdrop_path") val backdropPath: String? = null,
    val overview: String? = null,
    @Json(name = "first_air_date") val firstAirDate: String? = null,
    @Json(name = "vote_average") val voteAverage: Double? = null,
    @Json(name = "genre_ids") val genreIds: List<Int>? = null,
    @Json(name = "origin_country") val originCountry: List<String>? = null
)

fun TmdbMovie.toMediaItem(): MediaItem = MediaItem(
    id = "tmdb:movie:$id",
    title = title,
    posterUrl = posterPath?.let { "https://image.tmdb.org/t/p/w500$it" },
    backdropUrl = backdropPath?.let { "https://image.tmdb.org/t/p/w1280$it" },
    overview = overview,
    releaseDate = releaseDate,
    mediaType = MediaType.MOVIE,
    rating = voteAverage,
    source = CatalogSourceType.TMDB,
    year = releaseDate?.take(4)?.toIntOrNull()
)

fun TmdbTv.toMediaItem(): MediaItem = MediaItem(
    id = "tmdb:tv:$id",
    title = name,
    posterUrl = posterPath?.let { "https://image.tmdb.org/t/p/w500$it" },
    backdropUrl = backdropPath?.let { "https://image.tmdb.org/t/p/w1280$it" },
    overview = overview,
    releaseDate = firstAirDate,
    mediaType = MediaType.TV,
    rating = voteAverage,
    source = CatalogSourceType.TMDB,
    year = firstAirDate?.take(4)?.toIntOrNull()
)
