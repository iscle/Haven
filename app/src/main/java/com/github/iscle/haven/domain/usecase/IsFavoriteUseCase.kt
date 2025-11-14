package com.github.iscle.haven.domain.usecase

import com.github.iscle.haven.domain.repository.UnsplashRepository
import javax.inject.Inject

class IsFavoriteUseCase @Inject constructor(
    private val unsplashRepository: UnsplashRepository
) {
    suspend operator fun invoke(imageId: String): Boolean {
        return unsplashRepository.isFavorite(imageId)
    }
}


