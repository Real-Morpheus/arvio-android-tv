package com.arflix.tv.data.repository

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringSetPreferencesKey
import com.arflix.tv.data.model.CatalogConfig
import com.arflix.tv.data.model.CatalogSourceType
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class CatalogRepository @Inject constructor(
    private val dataStore: DataStore<Preferences>
) {

    companion object {
        private val ENABLED_CATALOGS_KEY = stringSetPreferencesKey("enabled_catalogs")

        private val DEFAULT_CATALOGS = listOf(
            CatalogConfig(
                id = "tmdb_popular_movies",
                name = "Popular Movies",
                sourceType = CatalogSourceType.TMDB
            ),
            CatalogConfig(
                id = "tmdb_popular_tv",
                name = "Popular TV Shows",
                sourceType = CatalogSourceType.TMDB
            )
        )
    }

    val catalogs: Flow<List<CatalogConfig>> = dataStore.data.map { prefs ->
        val enabled = prefs[ENABLED_CATALOGS_KEY]
        DEFAULT_CATALOGS.map { catalog ->
            catalog.copy(enabled = enabled == null || catalog.id in enabled)
        }
    }

    suspend fun toggleCatalog(catalogId: String, enabled: Boolean) {
        dataStore.edit { prefs ->
            val current = prefs[ENABLED_CATALOGS_KEY]?.toMutableSet()
                ?: DEFAULT_CATALOGS.map { it.id }.toMutableSet()
            if (enabled) current.add(catalogId) else current.remove(catalogId)
            prefs[ENABLED_CATALOGS_KEY] = current
        }
    }
}
