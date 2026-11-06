package com.github.iscle.haven.domain.usecase

import com.github.iscle.haven.domain.model.BackgroundImage
import com.github.iscle.haven.domain.repository.PreferencesRepository
import com.github.iscle.haven.domain.repository.UnsplashRepository
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.first
import timber.log.Timber
import javax.inject.Inject

/**
 * Picks a wallpaper from a random source the user has switched on. If that
 * source has nothing to give (an empty favourites list, a dead collection),
 * the remaining sources are tried before giving up.
 */
class GetRandomBackgroundImageUseCase @Inject constructor(
    private val unsplashRepository: UnsplashRepository,
    private val preferencesRepository: PreferencesRepository
) {
    suspend operator fun invoke(): Result<BackgroundImage> {
        val sources = preferencesRepository.photoSources.first().filter { it.enabled }
        Timber.d("UseCase: Getting random background image from ${sources.size} sources")

        val result = if (sources.isEmpty()) {
            // Nothing switched on: fall back to the search Haven shipped with.
            unsplashRepository.getRandomPhoto()
        } else {
            var attempt: Result<BackgroundImage> = Result.failure(
                IllegalStateException("No photo sources produced an image")
            )
            for (source in sources.shuffled()) {
                attempt = unsplashRepository.getRandomPhotoForSource(source)
                if (attempt.isSuccess) {
                    try {
                        preferencesRepository.setLastPhotoSourceId(source.id)
                    } catch (error: CancellationException) {
                        throw error
                    } catch (error: Exception) {
                        Timber.w(error, "Could not save last photo source")
                    }
                    break
                }
                Timber.w(
                    attempt.exceptionOrNull(),
                    "UseCase: Source '${source.label}' gave nothing, trying another"
                )
            }
            attempt
        }

        result.getOrNull()?.let { image ->
            try {
                unsplashRepository.addToHistory(image)
            } catch (error: CancellationException) {
                throw error
            } catch (error: Exception) {
                // History bookkeeping must not prevent an otherwise valid photo loading.
                Timber.w(error, "Could not save wallpaper history")
            }
        }
        return result
    }
}
