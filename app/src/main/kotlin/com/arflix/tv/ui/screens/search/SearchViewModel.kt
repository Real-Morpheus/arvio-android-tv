package com.arflix.tv.ui.screens.search

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.arflix.tv.data.model.MediaItem
import com.arflix.tv.data.repository.MediaRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class SearchUiState(
    val query: String = "",
    val movies: List<MediaItem> = emptyList(),
    val tvShows: List<MediaItem> = emptyList(),
    val isLoading: Boolean = false,
    val isDeepSearchLoading: Boolean = false,
    val error: String? = null,
    val isDeepSearchEnabled: Boolean = false,
    val hasDeepSearchResults: Boolean = false
)

@HiltViewModel
class SearchViewModel @Inject constructor(
    private val mediaRepository: MediaRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(SearchUiState())
    val uiState: StateFlow<SearchUiState> = _uiState.asStateFlow()

    private val _queryFlow = MutableStateFlow("")
    private var deepSearchJob: Job? = null

    init {
        observeQueryChanges()
    }

    @OptIn(FlowPreview::class)
    private fun observeQueryChanges() {
        _queryFlow
            .debounce(800L)
            .distinctUntilChanged()
            .filter { it.isNotBlank() }
            .onEach { query -> performSearch(query) }
            .launchIn(viewModelScope)
    }

    fun onQueryChanged(query: String) {
        _uiState.update { it.copy(query = query, error = null) }
        _queryFlow.value = query
        if (query.isBlank()) {
            _uiState.update { it.copy(movies = emptyList(), tvShows = emptyList(), isLoading = false) }
        }
    }

    fun toggleDeepSearch(enabled: Boolean) {
        _uiState.update { it.copy(isDeepSearchEnabled = enabled) }
        val currentQuery = _uiState.value.query
        if (currentQuery.isNotBlank()) {
            performSearch(currentQuery)
        }
    }

    fun retrySearch() {
        val query = _uiState.value.query
        if (query.isNotBlank()) {
            performSearch(query)
        }
    }

    private fun performSearch(query: String) {
        deepSearchJob?.cancel()
        deepSearchJob = viewModelScope.launch {
            val deepSearchEnabled = _uiState.value.isDeepSearchEnabled
            _uiState.update { it.copy(isLoading = true, error = null) }

            runCatching {
                if (deepSearchEnabled) {
                    _uiState.update { it.copy(isDeepSearchLoading = true) }
                    mediaRepository.deepSearch(query, includeCloudStream = true)
                } else {
                    mediaRepository.search(query)
                }
            }.onSuccess { results ->
                _uiState.update {
                    it.copy(
                        movies = results.movies,
                        tvShows = results.tvShows,
                        isLoading = false,
                        isDeepSearchLoading = false,
                        hasDeepSearchResults = deepSearchEnabled &&
                            (results.movies.isNotEmpty() || results.tvShows.isNotEmpty()),
                        error = null
                    )
                }
            }.onFailure { error ->
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        isDeepSearchLoading = false,
                        error = error.message ?: "Search failed"
                    )
                }
            }
        }
    }
}
