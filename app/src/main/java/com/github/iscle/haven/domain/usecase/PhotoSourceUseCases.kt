package com.github.iscle.haven.domain.usecase

import com.github.iscle.haven.domain.model.PhotoSource
import com.github.iscle.haven.domain.repository.PreferencesRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import javax.inject.Inject

class GetPhotoSourcesUseCase @Inject constructor(
    private val preferencesRepository: PreferencesRepository
) {
    operator fun invoke(): Flow<List<PhotoSource>> = preferencesRepository.photoSources
}

class SetPhotoSourcesUseCase @Inject constructor(
    private val preferencesRepository: PreferencesRepository
) {
    suspend operator fun invoke(sources: List<PhotoSource>) =
        preferencesRepository.setPhotoSources(sources)
}

/** Adds a source, or re-enables it when the same one is already listed. */
class AddPhotoSourceUseCase @Inject constructor(
    private val preferencesRepository: PreferencesRepository
) {
    suspend operator fun invoke(source: PhotoSource) {
        val current = preferencesRepository.photoSources.first()
        val existing = current.firstOrNull {
            it.type == source.type && it.value.equals(source.value, ignoreCase = true)
        }
        val updated = if (existing == null) {
            current + source
        } else {
            current.map { if (it.id == existing.id) it.copy(enabled = true) else it }
        }
        preferencesRepository.setPhotoSources(updated)
    }
}

class GetLastPhotoSourceIdUseCase @Inject constructor(
    private val preferencesRepository: PreferencesRepository
) {
    operator fun invoke(): Flow<String?> = preferencesRepository.lastPhotoSourceId
}

class RemovePhotoSourceUseCase @Inject constructor(
    private val preferencesRepository: PreferencesRepository
) {
    suspend operator fun invoke(sourceId: String) {
        val current = preferencesRepository.photoSources.first()
        preferencesRepository.setPhotoSources(current.filterNot { it.id == sourceId })
    }
}
