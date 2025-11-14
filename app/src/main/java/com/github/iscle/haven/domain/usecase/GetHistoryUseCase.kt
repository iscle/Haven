package com.github.iscle.haven.domain.usecase

import com.github.iscle.haven.domain.repository.UnsplashRepository
import com.github.iscle.haven.domain.model.WallpaperHistory
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class GetHistoryUseCase @Inject constructor(
    private val unsplashRepository: UnsplashRepository
) {
    operator fun invoke(): Flow<List<WallpaperHistory>> {
        return unsplashRepository.getHistory()
    }
}


