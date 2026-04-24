package com.example.petcare.util

import android.content.Context
import android.net.Uri
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

    suspend fun prepareImageIfNeeded(
        context: Context,
        sourceUri: Uri,
        mimeType: String,
        originalFileName: String
    ): PreparedUploadFile = withContext(Dispatchers.IO) {
        if (!mimeType.startsWith("image/")) {
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

        PreparedUploadFile(
            uri = Uri.fromFile(file),
            fileName = originalFileName.substringBeforeLast('.', originalFileName) + "_compressed.jpg",
            mimeType = "image/jpeg"
        )
    }
}
