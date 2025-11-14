package com.github.iscle.haven.domain.usecase

import com.github.iscle.haven.domain.repository.UnsplashRepository
import com.github.iscle.haven.domain.model.BackgroundImage
import timber.log.Timber
import javax.inject.Inject

class GetRandomBackgroundImageUseCase @Inject constructor(
    private val unsplashRepository: UnsplashRepository
) {
    suspend operator fun invoke(): Result<BackgroundImage> {
        Timber.d("UseCase: Getting random background image")
        return unsplashRepository.getRandomPhoto().mapCatching { backgroundImage ->
            Timber.d("UseCase: Successfully retrieved background image: id=${backgroundImage.id}, photographer=${backgroundImage.photographer}")
            
            // Add to history
            unsplashRepository.addToHistory(backgroundImage)
            
            backgroundImage
        }.onFailure { error ->
            Timber.e(error, "UseCase: Failed to get random background image")
        }
    }
}

