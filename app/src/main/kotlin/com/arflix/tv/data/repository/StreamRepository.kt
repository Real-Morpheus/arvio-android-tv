package com.arflix.tv.data.repository

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringSetPreferencesKey
import com.arflix.tv.data.api.StreamApi
import com.arflix.tv.data.model.Addon
import com.arflix.tv.data.model.AddonManifest
import com.arflix.tv.data.model.AddonType
import com.arflix.tv.data.model.StreamSource
import com.squareup.moshi.Moshi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class StreamRepository @Inject constructor(
    private val dataStore: DataStore<Preferences>,
    private val okHttpClient: OkHttpClient,
    private val moshi: Moshi
) {

    companion object {
        private val ADDON_URLS_KEY = stringSetPreferencesKey("stremio_addon_urls")
        private const val OFFICIAL_CINEMETA_URL = "https://v3-cinemeta.strem.io/"
    }

    val addons: Flow<List<Addon>> = dataStore.data.map { prefs ->
        val urls = prefs[ADDON_URLS_KEY] ?: setOf(OFFICIAL_CINEMETA_URL)
        urls.mapNotNull { url -> loadAddonFromCache(url) }
    }

    suspend fun addAddon(url: String): Result<Addon> = runCatching {
        val normalizedUrl = if (url.endsWith("/")) url else "$url/"
        val api = buildStreamApi(normalizedUrl)
        val manifest = api.getManifest()
        val addonType = determineAddonType(manifest)
        val addon = Addon(url = normalizedUrl, manifest = manifest, type = addonType)

        dataStore.edit { prefs ->
            val current = prefs[ADDON_URLS_KEY] ?: setOf(OFFICIAL_CINEMETA_URL)
            prefs[ADDON_URLS_KEY] = current + normalizedUrl
        }
        cacheAddon(normalizedUrl, manifest)
        addon
    }

    suspend fun removeAddon(url: String) {
        dataStore.edit { prefs ->
            val current = prefs[ADDON_URLS_KEY] ?: emptySet()
            prefs[ADDON_URLS_KEY] = current - url
        }
    }

    suspend fun getStreams(imdbId: String, type: String): List<StreamSource> {
        val urls = dataStore.data.first().let { prefs ->
            prefs[ADDON_URLS_KEY] ?: setOf(OFFICIAL_CINEMETA_URL)
        }
        val results = mutableListOf<StreamSource>()

        for (url in urls) {
            runCatching {
                val api = buildStreamApi(url)
                val response = if (type == "movie") {
                    api.getMovieStreams(imdbId)
                } else {
                    api.getSeriesStreams(imdbId)
                }
                response.streams?.let { results.addAll(it) }
            }
        }
        return results
    }

    suspend fun getStreamsFromAddon(addonUrl: String, imdbId: String, type: String): List<StreamSource> =
        runCatching {
            val api = buildStreamApi(addonUrl)
            val response = if (type == "movie") {
                api.getMovieStreams(imdbId)
            } else {
                api.getSeriesStreams(imdbId)
            }
            response.streams ?: emptyList()
        }.getOrDefault(emptyList())

    private fun determineAddonType(manifest: AddonManifest): AddonType {
        val knownOfficialIds = setOf("com.linvo.cinemeta", "com.stremio.local")
        return when {
            manifest.id in knownOfficialIds -> AddonType.OFFICIAL
            manifest.resources?.any { it.contains("subtitles") } == true -> AddonType.SUBTITLE
            else -> AddonType.COMMUNITY
        }
    }

    private fun buildStreamApi(baseUrl: String): StreamApi {
        val url = if (baseUrl.endsWith("/")) baseUrl else "$baseUrl/"
        return Retrofit.Builder()
            .baseUrl(url)
            .client(okHttpClient)
            .addConverterFactory(MoshiConverterFactory.create(moshi))
            .build()
            .create(StreamApi::class.java)
    }

    private suspend fun cacheAddon(url: String, manifest: AddonManifest) {
        val key = stringSetPreferencesKey("addon_manifest_$url")
        val adapter = moshi.adapter(AddonManifest::class.java)
        dataStore.edit { prefs ->
            prefs[key] = setOf(adapter.toJson(manifest))
        }
    }

    private suspend fun loadAddonFromCache(url: String): Addon? {
        return runCatching {
            val api = buildStreamApi(url)
            val manifest = api.getManifest()
            Addon(url = url, manifest = manifest, type = determineAddonType(manifest))
        }.getOrNull()
    }
}
