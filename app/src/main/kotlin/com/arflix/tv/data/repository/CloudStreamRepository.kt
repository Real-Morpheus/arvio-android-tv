package com.arflix.tv.data.repository

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.stringSetPreferencesKey
import com.arflix.tv.data.api.CloudStreamApi
import com.arflix.tv.data.model.CatalogSourceType
import com.arflix.tv.data.model.CloudStreamExtension
import com.arflix.tv.data.model.CloudStreamExtensionManifest
import com.arflix.tv.data.model.CloudStreamPluginEntry
import com.arflix.tv.data.model.CloudStreamSearchResult
import com.arflix.tv.data.model.MediaItem
import com.arflix.tv.data.model.MediaType
import com.squareup.moshi.Moshi
import com.squareup.moshi.Types
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withTimeoutOrNull
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class CloudStreamRepository @Inject constructor(
    private val cloudStreamApi: CloudStreamApi,
    private val dataStore: DataStore<Preferences>,
    private val moshi: Moshi
) {

    companion object {
        private val REPO_URLS_KEY = stringSetPreferencesKey("cloudstream_repo_urls")
        private const val SEARCH_TIMEOUT_MS = 8_000L
    }

    val extensions: Flow<List<CloudStreamExtension>> = dataStore.data.map { prefs ->
        val urls = prefs[REPO_URLS_KEY] ?: emptySet()
        urls.mapNotNull { url -> loadExtensionFromPrefs(url, prefs) }
    }

    suspend fun addExtensionRepository(repositoryUrl: String): Result<CloudStreamExtension> =
        runCatching {
            val manifest = loadManifest(repositoryUrl)
            val plugins = loadPluginsForManifest(repositoryUrl, manifest)
            val extension = CloudStreamExtension(
                repositoryUrl = repositoryUrl,
                manifest = manifest,
                plugins = plugins
            )
            persistExtension(repositoryUrl, manifest, plugins)
            extension
        }

    suspend fun removeExtensionRepository(repositoryUrl: String) {
        dataStore.edit { prefs ->
            val current = prefs[REPO_URLS_KEY] ?: emptySet()
            prefs[REPO_URLS_KEY] = current - repositoryUrl
            prefs.remove(stringPreferencesKey("cs_manifest_$repositoryUrl"))
            prefs.remove(stringPreferencesKey("cs_plugins_$repositoryUrl"))
        }
    }

    suspend fun getAvailablePlugins(): List<CloudStreamPluginEntry> {
        val prefs = dataStore.data.first()
        val urls = prefs[REPO_URLS_KEY] ?: emptySet()
        return urls.flatMap { url ->
            val json = prefs[stringPreferencesKey("cs_plugins_$url")] ?: return@flatMap emptyList()
            runCatching {
                val type = Types.newParameterizedType(List::class.java, CloudStreamPluginEntry::class.java)
                val adapter = moshi.adapter<List<CloudStreamPluginEntry>>(type)
                adapter.fromJson(json) ?: emptyList()
            }.getOrDefault(emptyList())
        }
    }

    suspend fun searchAll(query: String): List<MediaItem> = coroutineScope {
        val prefs = dataStore.data.first()
        val urls = prefs[REPO_URLS_KEY] ?: emptySet()
        if (urls.isEmpty()) return@coroutineScope emptyList()

        val plugins = urls.flatMap { url ->
            val json = prefs[stringPreferencesKey("cs_plugins_$url")] ?: return@flatMap emptyList()
            runCatching {
                val type = Types.newParameterizedType(List::class.java, CloudStreamPluginEntry::class.java)
                val adapter = moshi.adapter<List<CloudStreamPluginEntry>>(type)
                adapter.fromJson(json) ?: emptyList()
            }.getOrDefault(emptyList())
        }

        plugins.map { plugin ->
            async {
                withTimeoutOrNull(SEARCH_TIMEOUT_MS) {
                    searchPlugin(plugin, query)
                } ?: emptyList()
            }
        }.awaitAll().flatten()
    }

    suspend fun searchPlugin(plugin: CloudStreamPluginEntry, query: String): List<MediaItem> =
        runCatching {
            val searchUrl = buildSearchUrl(plugin.url, query)
            val response = cloudStreamApi.searchRaw(searchUrl)
            val results = response.results ?: emptyList()
            results.map { it.toMediaItem(plugin.name) }
        }.getOrDefault(emptyList())

    private fun buildSearchUrl(pluginUrl: String, query: String): String {
        val base = pluginUrl.trimEnd('/')
        return "$base/search?query=${query.encodeUrlComponent()}"
    }

    private suspend fun loadManifest(repositoryUrl: String): CloudStreamExtensionManifest =
        cloudStreamApi.getRepositoryManifest(repositoryUrl)

    private suspend fun loadPluginsForManifest(
        repositoryUrl: String,
        manifest: CloudStreamExtensionManifest
    ): List<CloudStreamPluginEntry> {
        val pluginListUrls = manifest.pluginLists ?: return emptyList()
        return pluginListUrls.flatMap { listUrl ->
            runCatching {
                val response = cloudStreamApi.getPluginList(resolveUrl(repositoryUrl, listUrl))
                response.plugins ?: response.allPlugins ?: emptyList()
            }.getOrDefault(emptyList())
        }
    }

    private fun resolveUrl(base: String, relative: String): String {
        if (relative.startsWith("http://") || relative.startsWith("https://")) return relative
        val baseWithSlash = if (base.endsWith("/")) base else "$base/"
        return baseWithSlash + relative.trimStart('/')
    }

    private suspend fun persistExtension(
        repositoryUrl: String,
        manifest: CloudStreamExtensionManifest,
        plugins: List<CloudStreamPluginEntry>
    ) {
        val manifestAdapter = moshi.adapter(CloudStreamExtensionManifest::class.java)
        val pluginsType = Types.newParameterizedType(List::class.java, CloudStreamPluginEntry::class.java)
        val pluginsAdapter = moshi.adapter<List<CloudStreamPluginEntry>>(pluginsType)

        dataStore.edit { prefs ->
            val current = prefs[REPO_URLS_KEY] ?: emptySet()
            prefs[REPO_URLS_KEY] = current + repositoryUrl
            prefs[stringPreferencesKey("cs_manifest_$repositoryUrl")] = manifestAdapter.toJson(manifest)
            prefs[stringPreferencesKey("cs_plugins_$repositoryUrl")] = pluginsAdapter.toJson(plugins)
        }
    }

    private fun loadExtensionFromPrefs(url: String, prefs: Preferences): CloudStreamExtension? {
        val manifestJson = prefs[stringPreferencesKey("cs_manifest_$url")] ?: return null
        return runCatching {
            val manifestAdapter = moshi.adapter(CloudStreamExtensionManifest::class.java)
            val manifest = manifestAdapter.fromJson(manifestJson) ?: return null

            val pluginsJson = prefs[stringPreferencesKey("cs_plugins_$url")]
            val plugins = if (pluginsJson != null) {
                val type = Types.newParameterizedType(List::class.java, CloudStreamPluginEntry::class.java)
                val adapter = moshi.adapter<List<CloudStreamPluginEntry>>(type)
                adapter.fromJson(pluginsJson) ?: emptyList()
            } else {
                emptyList()
            }

            CloudStreamExtension(repositoryUrl = url, manifest = manifest, plugins = plugins)
        }.getOrNull()
    }

    private fun String.encodeUrlComponent(): String =
        java.net.URLEncoder.encode(this, "UTF-8")

    private fun CloudStreamSearchResult.toMediaItem(sourceName: String): MediaItem = MediaItem(
        id = id ?: "cs:${name.hashCode()}",
        title = name,
        posterUrl = posterUrl,
        mediaType = when (type?.lowercase()) {
            "tvseries", "anime", "asiandrama" -> MediaType.TV
            else -> MediaType.MOVIE
        },
        source = CatalogSourceType.CLOUDSTREAM,
        sourceLabel = sourceName,
        year = year
    )
}
