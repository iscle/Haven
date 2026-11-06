package com.github.iscle.haven.data.local

import androidx.room.Room
import androidx.test.platform.app.InstrumentationRegistry
import com.github.iscle.haven.data.local.database.HavenDatabase
import com.github.iscle.haven.data.local.database.entity.CacheMetadataEntity
import com.github.iscle.haven.data.local.database.entity.PhotoCacheEntity
import com.github.iscle.haven.domain.model.BackgroundImage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.*
import org.junit.Test

class PhotoCacheTest {
    private val database = Room.inMemoryDatabaseBuilder(
        InstrumentationRegistry.getInstrumentation().targetContext,
        HavenDatabase::class.java
    ).build()
    private val dao = database.photoCacheDao()
    private val cache = PhotoCacheDataSource(dao)
    private fun image(id: String) = BackgroundImage(id, "url-$id", "Author", "author")

    @After fun close() = database.close()

    @Test fun cachedBatchIsConsumedWithoutRepeatsEvenWithConcurrentReaders() = runBlocking {
        val photos = (1..20).map { image(it.toString()) }
        cache.cachePhotos(" Nature ", photos)
        val consumed = (1..20).map {
            async(Dispatchers.IO) { cache.getRandomCachedPhoto("nature") }
        }.awaitAll()
        assertEquals(photos.toSet(), consumed.toSet())
        assertNull(cache.getRandomCachedPhoto("nature"))
        assertEquals(0, dao.getPhotoCount("nature"))
    }

    @Test fun expiredCacheIsRemoved() = runBlocking {
        cache.cachePhotos("nature", listOf(image("one")))
        dao.insertCacheMetadata(CacheMetadataEntity("nature", 0))
        assertNull(cache.getRandomCachedPhoto("nature"))
        assertNull(dao.getCacheMetadata("nature"))
    }

    @Test fun failedReplacementRollsBackToPreviousBatch() = runBlocking {
        cache.cachePhotos("nature", listOf(image("one")))
        try {
            dao.replacePhotos(
                CacheMetadataEntity("nature", System.currentTimeMillis()),
                listOf(PhotoCacheEntity("two", "missing-key", "url", "author", "author", cachedAt = 0))
            )
            fail("Expected foreign key failure")
        } catch (_: android.database.sqlite.SQLiteConstraintException) { }
        assertEquals(image("one"), cache.getRandomCachedPhoto("nature"))
    }
}
