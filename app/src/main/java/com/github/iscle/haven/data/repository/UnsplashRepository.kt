package com.github.iscle.haven.data.repository

import com.github.iscle.haven.data.local.HistoryDataSource
import com.github.iscle.haven.data.local.PhotoCacheDataSource
import com.github.iscle.haven.data.remote.UnsplashApiService
import com.github.iscle.haven.data.remote.model.UnsplashPhoto
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
        private const val PAGE_SIZE = 20

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

        val info = unsplashApiService.getCollection(collectionId)
            .getOrElse { return Result.failure(it) }
        val pages = ((info.totalPhotos + PAGE_SIZE - 1) / PAGE_SIZE).coerceIn(1, 50)
        for (page in (1..pages).shuffled().take(MAX_PAGE_RETRIES)) {
            val photos = unsplashApiService.getCollectionPhotos(collectionId, page, PAGE_SIZE)
                .getOrElse { return Result.failure(it) }
            cacheAndSelect(cacheKey, photos)?.let { return Result.success(it) }
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
        // Use the same page size for pagination metadata and subsequent pages.
        val firstPage = unsplashApiService.searchPhotos(query, page = 1, perPage = PAGE_SIZE)
            .getOrElse { return Result.failure(it) }
        val pages = (1..firstPage.totalPages.coerceIn(1, 50)).shuffled().take(MAX_PAGE_RETRIES)
        for (page in pages) {
            val response = if (page == 1) firstPage else {
                unsplashApiService.searchPhotos(query, page, PAGE_SIZE)
                    .getOrElse { return Result.failure(it) }
            }
            cacheAndSelect(query, response.results)?.let { return Result.success(it) }
        }
        return Result.failure(IllegalStateException("No landscape photos for query: $query"))
    }

    /** Cache the complete batch, then consume the selected photo exactly once. */
    private suspend fun cacheAndSelect(
        cacheKey: String,
        photos: List<UnsplashPhoto>
    ): BackgroundImage? {
        val images = photos.filter { it.width > it.height }.map(::mapToDomainModel)
        if (images.isEmpty()) return null
        photoCacheDataSource.cachePhotos(cacheKey, images)
        return photoCacheDataSource.getRandomCachedPhoto(cacheKey)
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

