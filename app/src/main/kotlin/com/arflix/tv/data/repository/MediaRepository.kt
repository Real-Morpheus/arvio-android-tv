package com.arflix.tv.data.repository

import com.arflix.tv.data.api.TmdbApi
import com.arflix.tv.data.model.MediaItem
import com.arflix.tv.data.model.MediaType
import com.arflix.tv.data.model.SearchResults
import com.arflix.tv.data.model.toMediaItem
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.withTimeoutOrNull
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class MediaRepository @Inject constructor(
    private val tmdbApi: TmdbApi,
    private val cloudStreamRepository: CloudStreamRepository
) {

    suspend fun search(query: String): SearchResults = coroutineScope {
        val moviesDeferred = async {
            runCatching { tmdbApi.searchMovies(query).results.map { it.toMediaItem() } }
                .getOrDefault(emptyList())
        }
        val tvDeferred = async {
            runCatching { tmdbApi.searchTv(query).results.map { it.toMediaItem() } }
                .getOrDefault(emptyList())
        }

        SearchResults(
            movies = moviesDeferred.await(),
            tvShows = tvDeferred.await()
        )
    }

    suspend fun deepSearch(query: String, includeCloudStream: Boolean = true): SearchResults =
        coroutineScope {
            val tmdbMoviesDeferred = async {
                runCatching { tmdbApi.searchMovies(query).results.map { it.toMediaItem() } }
                    .getOrDefault(emptyList())
            }
            val tmdbTvDeferred = async {
                runCatching { tmdbApi.searchTv(query).results.map { it.toMediaItem() } }
                    .getOrDefault(emptyList())
            }

            val cloudStreamDeferred = async {
                if (includeCloudStream) {
                    withTimeoutOrNull(10_000L) {
                        runCatching { cloudStreamRepository.searchAll(query) }
                            .getOrDefault(emptyList())
                    } ?: emptyList()
                } else {
                    emptyList()
                }
            }

            val tmdbMovies = tmdbMoviesDeferred.await()
            val tmdbTv = tmdbTvDeferred.await()
            val cloudStreamResults = cloudStreamDeferred.await()

            val cloudStreamMovies = cloudStreamResults.filter {
                it.mediaType == MediaType.MOVIE
            }
            val cloudStreamTv = cloudStreamResults.filter {
                it.mediaType == MediaType.TV
            }

            val mergedMovies = mergeResults(tmdbMovies, cloudStreamMovies)
            val mergedTv = mergeResults(tmdbTv, cloudStreamTv)

            SearchResults(
                movies = mergedMovies,
                tvShows = mergedTv
            )
        }

    suspend fun getPopularMovies(): List<MediaItem> =
        runCatching { tmdbApi.getPopularMovies().results.map { it.toMediaItem() } }
            .getOrDefault(emptyList())

    suspend fun getPopularTv(): List<MediaItem> =
        runCatching { tmdbApi.getPopularTv().results.map { it.toMediaItem() } }
            .getOrDefault(emptyList())

    private fun mergeResults(
        primary: List<MediaItem>,
        secondary: List<MediaItem>
    ): List<MediaItem> {
        val combined = primary.toMutableList()
        secondary.forEach { secondaryItem ->
            val isDuplicate = combined.any { existing ->
                existing.title.equals(secondaryItem.title, ignoreCase = true) &&
                    existing.year == secondaryItem.year
            }
            if (!isDuplicate) {
                combined.add(secondaryItem)
            }
        }
        return combined
    }
}
