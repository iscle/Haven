package com.github.iscle.haven.domain.usecase

import com.github.iscle.haven.domain.model.BackgroundImage
import com.github.iscle.haven.domain.repository.PreferencesRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

/** The wallpaper the user pinned from history with "Show on clock now". */
class GetPinnedImageUseCase @Inject constructor(
    private val preferencesRepository: PreferencesRepository
) {
    operator fun invoke(): Flow<BackgroundImage?> = preferencesRepository.pinnedImage
}

class SetPinnedImageUseCase @Inject constructor(
    private val preferencesRepository: PreferencesRepository
) {
    suspend operator fun invoke(image: BackgroundImage?) =
        preferencesRepository.setPinnedImage(image)
}
