package com.arflix.tv.ui.screens.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.arflix.tv.data.model.Addon
import com.arflix.tv.data.model.CloudStreamExtension
import com.arflix.tv.data.model.CloudStreamPluginEntry
import com.arflix.tv.data.repository.CloudStreamRepository
import com.arflix.tv.data.repository.StreamRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class SettingsUiState(
    val stremioAddons: List<Addon> = emptyList(),
    val cloudStreamExtensions: List<CloudStreamExtension> = emptyList(),
    val availablePlugins: List<CloudStreamPluginEntry> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null,
    val successMessage: String? = null
)

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val streamRepository: StreamRepository,
    private val cloudStreamRepository: CloudStreamRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(SettingsUiState())
    val uiState: StateFlow<SettingsUiState> = _uiState.asStateFlow()

    init {
        observeAddons()
        observeExtensions()
    }

    private fun observeAddons() {
        streamRepository.addons
            .onEach { addons ->
                _uiState.update { it.copy(stremioAddons = addons) }
            }
            .launchIn(viewModelScope)
    }

    private fun observeExtensions() {
        cloudStreamRepository.extensions
            .onEach { extensions ->
                _uiState.update { it.copy(cloudStreamExtensions = extensions) }
            }
            .launchIn(viewModelScope)
    }

    fun addStremioAddon(url: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            streamRepository.addAddon(url)
                .onSuccess { addon ->
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            successMessage = "Added Stremio addon: ${addon.manifest.name}"
                        )
                    }
                }
                .onFailure { error ->
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            error = "Failed to add addon: ${error.message}"
                        )
                    }
                }
        }
    }

    fun removeStremioAddon(url: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            runCatching { streamRepository.removeAddon(url) }
                .onSuccess {
                    _uiState.update {
                        it.copy(isLoading = false, successMessage = "Addon removed")
                    }
                }
                .onFailure { error ->
                    _uiState.update {
                        it.copy(isLoading = false, error = "Failed to remove addon: ${error.message}")
                    }
                }
        }
    }

    fun addCloudStreamRepository(url: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            cloudStreamRepository.addExtensionRepository(url)
                .onSuccess { extension ->
                    val plugins = cloudStreamRepository.getAvailablePlugins()
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            availablePlugins = plugins,
                            successMessage = "Added CloudStream repository: ${extension.manifest.name}" +
                                " (${extension.plugins.size} plugins)"
                        )
                    }
                }
                .onFailure { error ->
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            error = "Failed to add CloudStream repository: ${error.message}"
                        )
                    }
                }
        }
    }

    fun removeCloudStreamRepository(url: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            runCatching { cloudStreamRepository.removeExtensionRepository(url) }
                .onSuccess {
                    _uiState.update {
                        it.copy(isLoading = false, successMessage = "CloudStream repository removed")
                    }
                }
                .onFailure { error ->
                    _uiState.update {
                        it.copy(isLoading = false, error = "Failed to remove repository: ${error.message}")
                    }
                }
        }
    }

    fun refreshPlugins() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            runCatching { cloudStreamRepository.getAvailablePlugins() }
                .onSuccess { plugins ->
                    _uiState.update { it.copy(isLoading = false, availablePlugins = plugins) }
                }
                .onFailure { error ->
                    _uiState.update {
                        it.copy(isLoading = false, error = "Failed to refresh plugins: ${error.message}")
                    }
                }
        }
    }

    fun clearMessage() {
        _uiState.update { it.copy(error = null, successMessage = null) }
    }
}
