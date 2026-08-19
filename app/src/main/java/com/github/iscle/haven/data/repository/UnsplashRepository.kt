package com.github.iscle.haven.data.repository

import com.github.iscle.haven.data.local.HistoryDataSource
import com.github.iscle.haven.data.local.PhotoCacheDataSource
import com.github.iscle.haven.data.remote.UnsplashApiService
import com.github.iscle.haven.data.remote.model.UnsplashPhoto
import com.github.iscle.haven.data.remote.model.UnsplashSearchResponse
import com.github.iscle.haven.domain.model.BackgroundImage
import com.github.iscle.haven.domain.model.PhotoSource
import com.github.iscle.haven.domain.model.PhotoSourceType
import com.github.iscle.haven.domain.model.WallpaperHistory
import kotlinx.coroutines.flow.Flow
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton
import com.github.iscle.haven.domain.repository.UnsplashRepository as UnsplashRepositoryInterface

@Singleton
class UnsplashRepositoryImpl @Inject constructor(
    private val unsplashApiService: UnsplashApiService,
    private val photoCacheDataSource: PhotoCacheDataSource,
    private val historyDataSource: HistoryDataSource
) : UnsplashRepositoryInterface {
    companion object {
        private const val MAX_PAGE_RETRIES = 5
        private const val COLLECTION_PAGE_SIZE = 20

        fun collectionCacheKey(collectionId: String): String = "collection:$collectionId"
    }

    override suspend fun getRandomPhoto(
        query: String,
        includeFavorites: Boolean
    ): Result<BackgroundImage> {
        Timber.d("Repository: Getting random photo, query=$query, includeFavorites=$includeFavorites")

        // Occasionally show a favorite (10% chance if favorites exist)
        if (includeFavorites) {
            val favorites = historyDataSource.getFavorites()
            if (favorites.isNotEmpty() && (0..9).random() == 0) {
                val favorite = favorites.random()
                Timber.d("Repository: Showing favorite photo: id=${favorite.id}")
                return Result.success(favorite)
            }
        }
        
        // Try to get from cache first (rotates through cached photos)
        val cachedPhoto = photoCacheDataSource.getRandomCachedPhoto(query)
        if (cachedPhoto != null) {
            Timber.d("Repository: Using cached photo: id=${cachedPhoto.id}")
            return Result.success(cachedPhoto)
        }
        
        // If cache is empty or expired, fetch a new page from API
        Timber.d("Repository: Cache empty, fetching new photos from API")
        return fetchAndCachePhotos(query)
    }
    
    override suspend fun getRandomPhotoForSource(source: PhotoSource): Result<BackgroundImage> {
        Timber.d("Repository: Getting a photo for source ${source.type} '${source.label}'")
        return when (source.type) {
            PhotoSourceType.UNSPLASH_QUERY -> getRandomPhoto(
                query = source.value,
                includeFavorites = false
            )

            PhotoSourceType.UNSPLASH_COLLECTION -> getRandomCollectionPhoto(source.value)

            PhotoSourceType.FAVORITES -> {
                val favorite = historyDataSource.getFavorites().randomOrNull()
                if (favorite == null) {
                    Result.failure(IllegalStateException("No favorites yet"))
                } else {
                    Result.success(favorite)
                }
            }

            PhotoSourceType.DEVICE -> Result.success(
                BackgroundImage(
                    id = source.id,
                    url = source.value,
                    photographer = source.label,
                    photographerUsername = ""
                )
            )
        }
    }

    override suspend fun getCollectionInfo(collectionId: String): Result<Pair<String, Int>> =
        unsplashApiService.getCollection(collectionId).map { it.title to it.totalPhotos }

    /** Collections are paged like search, and cached under their own key. */
    private suspend fun getRandomCollectionPhoto(collectionId: String): Result<BackgroundImage> {
        val cacheKey = collectionCacheKey(collectionId)
        photoCacheDataSource.getRandomCachedPhoto(cacheKey)?.let { cached ->
            Timber.d("Repository: Using cached collection photo: id=${cached.id}")
            return Result.success(cached)
        }

        val info = unsplashApiService.getCollection(collectionId).getOrNull()
        val totalPhotos = info?.totalPhotos ?: 0
        val pages = if (totalPhotos > 0) {
            ((totalPhotos + COLLECTION_PAGE_SIZE - 1) / COLLECTION_PAGE_SIZE).coerceAtMost(50)
        } else {
            1
        }

        repeat(MAX_PAGE_RETRIES) {
            val page = (1..pages).random()
            val photos = unsplashApiService
                .getCollectionPhotos(collectionId, page = page, perPage = COLLECTION_PAGE_SIZE)
                .getOrNull()
                ?.filter { it.width > it.height }
                ?.map(::mapToDomainModel)
                .orEmpty()

            if (photos.isNotEmpty()) {
                photoCacheDataSource.cachePhotos(cacheKey, photos)
                return Result.success(photos.random())
            }
        }

        return Result.failure(
            IllegalStateException("No landscape photos in collection $collectionId")
        )
    }

    override suspend fun addToHistory(image: BackgroundImage) {
        historyDataSource.addToHistory(image)
    }
    
    override fun getHistory(): Flow<List<WallpaperHistory>> {
        return historyDataSource.getHistory()
    }
    
    override suspend fun toggleFavorite(imageId: String): Boolean {
        return historyDataSource.toggleFavorite(imageId)
    }
    
    override suspend fun isFavorite(imageId: String): Boolean {
        return historyDataSource.isFavorite(imageId)
    }
    
    override suspend fun getFavorites(): List<BackgroundImage> {
        return historyDataSource.getFavorites()
    }

    private suspend fun fetchAndCachePhotos(query: String): Result<BackgroundImage> {
        // First, fetch page 1 to get total pages
        val firstPageResult = unsplashApiService.searchPhotos(query, page = 1, perPage = 1)

        return firstPageResult.fold(
            onSuccess = { firstPageResponse ->
                if (firstPageResponse.totalPages == 0) {
                    Timber.w("Repository: No pages available for query: $query")
                    return@fold Result.failure(IllegalArgumentException("No photos found for query: $query"))
                }

                val landscapePhotoResult = fetchLandscapePhotoWithRetry(query, firstPageResponse)
                landscapePhotoResult.fold(
                    onSuccess = { photo ->
                        // Cache all photos from successful response, return random one
                        photoCacheDataSource.cachePhotos(query, listOf(photo))
                        Result.success(photo)
                    },
                    onFailure = { error ->
                        Result.failure(error)
                    }
                )
            },
            onFailure = { error ->
                Timber.e(error, "Repository: Failed to fetch first page to determine total pages")
                Result.failure(error)
            }
        )
    }

    private suspend fun fetchLandscapePhotoWithRetry(
        query: String,
        firstPageResponse: UnsplashSearchResponse
    ): Result<BackgroundImage> {
        val maxPages = minOf(firstPageResponse.totalPages, 50)
        val availablePages = (1..maxPages).toList()
        val triedPages = mutableSetOf<Int>()

        repeat(MAX_PAGE_RETRIES) { attempt ->
            val remainingPages = availablePages.filter { it !in triedPages }
            if (remainingPages.isEmpty()) {
                return Result.failure(IllegalArgumentException("No landscape photos found in any page"))
            }

            val randomPage = remainingPages.random()
            triedPages.add(randomPage)

            val pageResult = fetchLandscapePhotoFromPage(query, randomPage)
            if (pageResult.isSuccess) {
                return pageResult
            }

            // Log the failure and continue to next attempt
            val error = pageResult.exceptionOrNull()
            Timber.w(error, "Repository: Failed to fetch landscape photo from page $randomPage (attempt ${attempt + 1}/$MAX_PAGE_RETRIES)")
        }

        return Result.failure(IllegalArgumentException("No landscape photos found after trying $MAX_PAGE_RETRIES pages"))
    }

    private suspend fun fetchLandscapePhotoFromPage(query: String, page: Int): Result<BackgroundImage> {
        Timber.d("Repository: Fetching page $page for landscape photos")

        return unsplashApiService.searchPhotos(query, page = page, perPage = 20).fold(
            onSuccess = { response ->
                val landscapePhotos = response.results.filter { it.width > it.height }

                if (landscapePhotos.isEmpty()) {
                    Timber.w("Repository: No landscape photos found on page $page")
                    Result.failure(IllegalStateException("No landscape photos on page $page"))
                } else {
                    val backgroundImages = landscapePhotos.map { mapToDomainModel(it) }
                    // Cache all photos from this successful page
                    photoCacheDataSource.cachePhotos(query, backgroundImages)

                    val randomImage = backgroundImages.random()
                    Timber.d("Repository: Found landscape photo: id=${randomImage.id}")
                    Result.success(randomImage)
                }
            },
            onFailure = { error ->
                Timber.e(error, "Repository: Failed to fetch photos from page $page")
                Result.failure(error)
            }
        )
    }
    
    private fun mapToDomainModel(photo: UnsplashPhoto): BackgroundImage {
        return BackgroundImage(
            id = photo.id,
            url = photo.urls.full,
            photographer = photo.user.name,
            photographerUsername = photo.user.username,
            unsplashUrl = photo.links.html,
            artistProfileUrl = photo.user.links.html,
            profileImageUrl = photo.user.profileImage?.large
        )
    }
}

