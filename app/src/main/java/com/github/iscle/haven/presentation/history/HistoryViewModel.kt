package com.github.iscle.haven.presentation.history

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.github.iscle.haven.domain.model.WallpaperHistory
import com.github.iscle.haven.domain.usecase.GetHistoryUseCase
import com.github.iscle.haven.domain.usecase.IsFavoriteUseCase
import com.github.iscle.haven.domain.usecase.ToggleFavoriteUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject

data class HistoryUiState(
    val history: List<WallpaperHistory> = emptyList(),
    val isLoading: Boolean = false,
    val selectedImage: WallpaperHistory? = null,
    val showDetails: Boolean = false
)

@HiltViewModel
class HistoryViewModel @Inject constructor(
    private val getHistoryUseCase: GetHistoryUseCase,
    private val toggleFavoriteUseCase: ToggleFavoriteUseCase,
    private val isFavoriteUseCase: IsFavoriteUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(HistoryUiState())
    val uiState: StateFlow<HistoryUiState> = _uiState.asStateFlow()

    init {
        Timber.d("HistoryViewModel: Initializing")
        loadHistory()
    }

    private fun loadHistory() {
        viewModelScope.launch {
            Timber.d("HistoryViewModel: Loading history")
            _uiState.update { it.copy(isLoading = true) }
            
            getHistoryUseCase().collect { history ->
                Timber.d("HistoryViewModel: History loaded: ${history.size} entries")
                _uiState.update {
                    it.copy(
                        history = history,
                        isLoading = false
                    )
                }
            }
        }
    }

    fun toggleFavorite(imageId: String) {
        viewModelScope.launch {
            Timber.d("HistoryViewModel: Toggling favorite for image: $imageId")
            val isFavorite = toggleFavoriteUseCase(imageId)
            
            // Update the history entry
            _uiState.update { state ->
                state.copy(
                    history = state.history.map { entry ->
                        if (entry.image.id == imageId) {
                            entry.copy(isFavorite = isFavorite)
                        } else {
                            entry
                        }
                    },
                    selectedImage = state.selectedImage?.let { selected ->
                        if (selected.image.id == imageId) {
                            selected.copy(isFavorite = isFavorite)
                        } else {
                            selected
                        }
                    }
                )
            }
        }
    }

    fun showDetails(image: WallpaperHistory) {
        Timber.d("HistoryViewModel: Showing details for image: ${image.image.id}")
        _uiState.update {
            it.copy(
                selectedImage = image,
                showDetails = true
            )
        }
    }

    fun hideDetails() {
        Timber.d("HistoryViewModel: Hiding details")
        _uiState.update {
            it.copy(
                showDetails = false,
                selectedImage = null
            )
        }
    }
}


