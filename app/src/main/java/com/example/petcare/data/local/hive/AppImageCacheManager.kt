package com.example.petcare.data.local.hive


import android.content.Context
import coil.ImageLoader
import coil.disk.DiskCache
import coil.memory.MemoryCache
import coil.request.CachePolicy
import java.io.File

/**
 * Caché de imágenes en disco usando Coil.
 * TTL: 7 días — configurado via DiskCache.
 * Se inicializa una sola vez en Application.onCreate()
 * y todos los fragments lo usan via Coil.imageLoader(context).
 */
object AppImageCacheManager {

    private const val CACHE_DIR = "app_image_cache"
    private const val MAX_SIZE_BYTES = 50L * 1024 * 1024  // 50 MB
    private const val TTL_7_DAYS_MS = 7L * 24 * 60 * 60 * 1000

    fun buildImageLoader(context: Context): ImageLoader {
        return ImageLoader.Builder(context)
            .diskCachePolicy(CachePolicy.ENABLED)
            .memoryCachePolicy(CachePolicy.ENABLED)
            .memoryCache {
                MemoryCache.Builder(context)
                    .maxSizePercent(0.25) // 25% de la memoria disponible
                    .build()
            }
            .diskCache {
                DiskCache.Builder()
                    .directory(File(context.cacheDir, CACHE_DIR))
                    .maxSizeBytes(MAX_SIZE_BYTES)
                    .build()
            }
            .build()
    }

    /**
     * Limpia entradas del disco más antiguas que 7 días.
     * Llamar desde WorkManager periódicamente.
     */
    fun clearExpired(context: Context) {
        val cacheDir = File(context.cacheDir, CACHE_DIR)
        if (!cacheDir.exists()) return
        val cutoff = System.currentTimeMillis() - TTL_7_DAYS_MS
        cacheDir.listFiles()
            ?.filter { it.lastModified() < cutoff }
            ?.forEach { it.delete() }
    }
}