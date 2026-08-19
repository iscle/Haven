package com.github.iscle.haven.presentation.photosource

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.github.iscle.haven.domain.model.AppPreferences
import com.github.iscle.haven.domain.model.PhotoSource
import com.github.iscle.haven.domain.model.PhotoSourceType
import com.github.iscle.haven.domain.repository.UnsplashRepository
import com.github.iscle.haven.domain.usecase.AddPhotoSourceUseCase
import com.github.iscle.haven.domain.usecase.GetAppPreferencesUseCase
import com.github.iscle.haven.domain.usecase.GetLastPhotoSourceIdUseCase
import com.github.iscle.haven.domain.usecase.GetPhotoSourcesUseCase
import com.github.iscle.haven.domain.usecase.RemovePhotoSourceUseCase
import com.github.iscle.haven.domain.usecase.SetPhotoSourcesUseCase
import com.github.iscle.haven.domain.usecase.SetShowPhotographerCreditUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject

/** The two halves of the source picker. */
enum class PhotoSourceTab { UNSPLASH, DEVICE }

/** Query suggestions offered under the input, straight from the design. */
val PHOTO_SOURCE_SUGGESTIONS = listOf(
    "minimal",
    "architecture",
    "black and white",
    "nordic",
    "night sky",
    "botanical"
)

data class PhotoSourceUiState(
    val sources: List<PhotoSource> = emptyList(),
    val preferences: AppPreferences = AppPreferences(),
    val tab: PhotoSourceTab = PhotoSourceTab.UNSPLASH,
    val input: String = "",
    val isAdding: Boolean = false,
    val errorMessage: String? = null,
    val activeSourceId: String? = null
) {
    val visibleSources: List<PhotoSource>
        get() = when (tab) {
            PhotoSourceTab.UNSPLASH -> sources.filter { it.type != PhotoSourceType.DEVICE }
            PhotoSourceTab.DEVICE -> sources.filter { it.type == PhotoSourceType.DEVICE }
        }

    val enabledCount: Int get() = sources.count { it.enabled }
}

@HiltViewModel
class PhotoSourceViewModel @Inject constructor(
    private val getPhotoSourcesUseCase: GetPhotoSourcesUseCase,
    private val setPhotoSourcesUseCase: SetPhotoSourcesUseCase,
    private val addPhotoSourceUseCase: AddPhotoSourceUseCase,
    private val removePhotoSourceUseCase: RemovePhotoSourceUseCase,
    private val getAppPreferencesUseCase: GetAppPreferencesUseCase,
    private val setShowPhotographerCreditUseCase: SetShowPhotographerCreditUseCase,
    private val getLastPhotoSourceIdUseCase: GetLastPhotoSourceIdUseCase,
    private val unsplashRepository: UnsplashRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(PhotoSourceUiState())
    val uiState: StateFlow<PhotoSourceUiState> = _uiState.asStateFlow()

    init {
        Timber.d("PhotoSourceViewModel: Initializing")
        viewModelScope.launch {
            getPhotoSourcesUseCase().collect { sources ->
                _uiState.update { it.copy(sources = sources) }
                loadMissingPreviews(sources)
            }
        }
        viewModelScope.launch {
            getAppPreferencesUseCase().collect { preferences ->
                _uiState.update { it.copy(preferences = preferences) }
            }
        }
        viewModelScope.launch {
            getLastPhotoSourceIdUseCase().collect { id ->
                _uiState.update { it.copy(activeSourceId = id) }
            }
        }
    }

    fun selectTab(tab: PhotoSourceTab) {
        _uiState.update { it.copy(tab = tab, errorMessage = null) }
    }

    fun onInputChange(value: String) {
        _uiState.update { it.copy(input = value, errorMessage = null) }
    }

    fun setShowPhotographerCredit(enabled: Boolean) {
        viewModelScope.launch { setShowPhotographerCreditUseCase(enabled) }
    }

    fun setEnabled(sourceId: String, enabled: Boolean) {
        viewModelScope.launch {
            val updated = _uiState.value.sources.map {
                if (it.id == sourceId) it.copy(enabled = enabled) else it
            }
            setPhotoSourcesUseCase(updated)
        }
    }

    fun remove(sourceId: String) {
        viewModelScope.launch { removePhotoSourceUseCase(sourceId) }
    }

    fun addSuggestion(query: String) {
        addQuery(query)
    }

    /** Accepts a search phrase, a collection URL, or a bare collection id. */
    fun addFromInput() {
        val raw = _uiState.value.input.trim()
        if (raw.isBlank()) return

        val collectionId = parseCollectionId(raw)
        if (collectionId == null) {
            addQuery(raw)
            return
        }

        viewModelScope.launch {
            _uiState.update { it.copy(isAdding = true, errorMessage = null) }
            unsplashRepository.getCollectionInfo(collectionId).fold(
                onSuccess = { (title, totalPhotos) ->
                    addPhotoSourceUseCase(
                        PhotoSource(
                            id = "collection-$collectionId",
                            type = PhotoSourceType.UNSPLASH_COLLECTION,
                            label = title,
                            value = collectionId,
                            subtitle = "Collection · $totalPhotos photos"
                        )
                    )
                    _uiState.update { it.copy(input = "", isAdding = false) }
                },
                onFailure = { error ->
                    Timber.w(error, "PhotoSourceViewModel: Could not read collection $collectionId")
                    _uiState.update {
                        it.copy(
                            isAdding = false,
                            errorMessage = "That collection could not be read"
                        )
                    }
                }
            )
        }
    }

    fun addDevicePhotos(uris: List<String>) {
        if (uris.isEmpty()) return
        viewModelScope.launch {
            uris.forEachIndexed { index, uri ->
                addPhotoSourceUseCase(
                    PhotoSource(
                        id = "device-${uri.hashCode()}",
                        type = PhotoSourceType.DEVICE,
                        label = "Photo ${_uiState.value.sources.count { it.type == PhotoSourceType.DEVICE } + index + 1}",
                        value = uri,
                        subtitle = "On this device",
                        previewUrl = uri
                    )
                )
            }
            _uiState.update { it.copy(tab = PhotoSourceTab.DEVICE) }
        }
    }

    private fun addQuery(query: String) {
        viewModelScope.launch {
            addPhotoSourceUseCase(
                PhotoSource(
                    id = "query-${query.lowercase().hashCode()}",
                    type = PhotoSourceType.UNSPLASH_QUERY,
                    label = query,
                    value = query,
                    subtitle = "Search query"
                )
            )
            _uiState.update { it.copy(input = "") }
        }
    }

    /**
     * Cards show a photo from their own source. The first one fetched is kept
     * so the pane does not hit the network every time it opens.
     */
    private fun loadMissingPreviews(sources: List<PhotoSource>) {
        val missing = sources.filter { it.previewUrl.isNullOrBlank() }
        if (missing.isEmpty()) return

        viewModelScope.launch {
            var updated = sources
            var changed = false
            for (source in missing) {
                val preview = unsplashRepository.getRandomPhotoForSource(source).getOrNull()
                    ?: continue
                updated = updated.map {
                    if (it.id == source.id) it.copy(previewUrl = preview.url) else it
                }
                changed = true
            }
            if (changed) setPhotoSourcesUseCase(updated)
        }
    }

    private fun parseCollectionId(input: String): String? {
        val fromUrl = Regex("collections/(\\d+)").find(input)?.groupValues?.get(1)
        if (fromUrl != null) return fromUrl
        return if (input.all { it.isDigit() } && input.length >= 4) input else null
    }
}
