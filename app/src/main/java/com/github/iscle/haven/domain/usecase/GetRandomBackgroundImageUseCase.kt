package com.github.iscle.haven.domain.usecase

import com.github.iscle.haven.data.repository.UnsplashRepository
import com.github.iscle.haven.domain.model.BackgroundImage
import timber.log.Timber
import javax.inject.Inject

class GetRandomBackgroundImageUseCase @Inject constructor(
    private val unsplashRepository: UnsplashRepository
) {
    suspend operator fun invoke(): Result<BackgroundImage> {
        Timber.d("UseCase: Getting random background image")
        return unsplashRepository.getRandomPhoto().mapCatching { photo ->
            val backgroundImage = BackgroundImage(
                id = photo.id,
                url = photo.urls.full,
                photographer = photo.user.name,
                photographerUsername = photo.user.username
            )
            Timber.d("UseCase: Successfully created background image: id=${backgroundImage.id}, photographer=${backgroundImage.photographer}")
            backgroundImage
        }.onFailure { error ->
            Timber.e(error, "UseCase: Failed to get random background image")
        }
    }
}

