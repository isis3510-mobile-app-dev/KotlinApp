package com.example.petcare.util

import android.content.Context
import android.net.Uri
import android.util.Log
import com.squareup.picasso.Picasso
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.util.UUID

data class PreparedUploadFile(
    val uri: Uri,
    val fileName: String,
    val mimeType: String
)

object PicassoImageCompressor {
    private const val MAX_EDGE_PX = 1600
    private const val JPEG_QUALITY = 82
    private const val TAG = "PICASSO_COMPRESS"

    suspend fun prepareImageIfNeeded(
        context: Context,
        sourceUri: Uri,
        mimeType: String,
        originalFileName: String
    ): PreparedUploadFile = withContext(Dispatchers.IO) {
        Log.d(
            TAG,
            "prepare start fileName=$originalFileName mimeType=$mimeType thread=${Thread.currentThread().name}"
        )
        try {
            if (!mimeType.startsWith("image/")) {
                Log.d(TAG, "SKIP non-image fileName=$originalFileName mimeType=$mimeType")
                return@withContext PreparedUploadFile(sourceUri, originalFileName, mimeType)
            }

            val bitmap = Picasso.get()
                .load(sourceUri)
                .resize(MAX_EDGE_PX, MAX_EDGE_PX)
                .centerInside()
                .onlyScaleDown()
                .get()

            val dir = File(context.filesDir, "compressed_uploads")
            dir.mkdirs()
            val file = File(dir, "${UUID.randomUUID()}.jpg")

            FileOutputStream(file).use { output ->
                bitmap.compress(android.graphics.Bitmap.CompressFormat.JPEG, JPEG_QUALITY, output)
            }
            bitmap.recycle()

            Log.d(
                TAG,
                "COMPRESSED fileName=$originalFileName output=${file.name} bytes=${file.length()} maxEdge=$MAX_EDGE_PX quality=$JPEG_QUALITY"
            )
            PreparedUploadFile(
                uri = Uri.fromFile(file),
                fileName = originalFileName.substringBeforeLast('.', originalFileName) + "_compressed.jpg",
                mimeType = "image/jpeg"
            )
        } catch (e: Exception) {
            Log.e(TAG, "FAILED fileName=$originalFileName mimeType=$mimeType: ${e.message}", e)
            throw e
        }
    }
}
