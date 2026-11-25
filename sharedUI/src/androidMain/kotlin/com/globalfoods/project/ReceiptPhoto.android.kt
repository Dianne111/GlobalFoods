package com.globalfoods.project

import android.content.ActivityNotFoundException
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.media.ExifInterface
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.FileProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import java.io.File

private const val READ_ERROR = "No se pudo leer la foto. Intenta tomarla de nuevo."

@Composable
internal actual fun rememberReceiptPhotoPicker(
    onPhoto: (ByteArray) -> Unit,
    onError: (String) -> Unit
): ReceiptPhotoPicker {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    // Archivo temporal donde la cámara deja la foto; se conserva si Android recrea la pantalla.
    var pendingPath by rememberSaveable { mutableStateOf<String?>(null) }

    fun process(uri: Uri, temporary: File?) {
        scope.launch {
            val jpeg = withContext(Dispatchers.IO) {
                runCatching { compressReceipt(context, uri) }.getOrNull().also { temporary?.delete() }
            }
            if (jpeg != null) onPhoto(jpeg) else onError(READ_ERROR)
        }
    }

    val camera = rememberLauncherForActivityResult(ActivityResultContracts.TakePicture()) { saved ->
        val file = pendingPath?.let(::File)
        pendingPath = null
        if (file == null) return@rememberLauncherForActivityResult
        if (saved) process(Uri.fromFile(file), file) else file.delete()
    }
    val gallery = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        if (uri != null) process(uri, null)
    }

    return remember(camera, gallery) {
        object : ReceiptPhotoPicker {
            override fun takePhoto() {
                val file = runCatching { newReceiptFile(context) }.getOrNull()
                    ?: return onError(READ_ERROR)
                pendingPath = file.absolutePath
                try {
                    camera.launch(FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file))
                } catch (_: ActivityNotFoundException) {
                    pendingPath = null
                    file.delete()
                    onError("Este teléfono no tiene una aplicación de cámara. Elige la foto de la galería.")
                }
            }

            override fun pickFromGallery() {
                gallery.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
            }
        }
    }
}

@Composable
internal actual fun ReceiptPhotoPreview(jpeg: ByteArray, modifier: Modifier) {
    val bitmap = remember(jpeg) { BitmapFactory.decodeByteArray(jpeg, 0, jpeg.size)?.asImageBitmap() }
    if (bitmap != null) {
        Image(bitmap = bitmap, contentDescription = "Foto del comprobante", contentScale = ContentScale.Crop, modifier = modifier)
    }
}

/** Fotos temporales de la cámara, en caché (se borran al procesarlas). Ruta declarada en receipt_paths.xml. */
private fun newReceiptFile(context: Context): File {
    val folder = File(context.cacheDir, "comprobantes").apply { mkdirs() }
    return File.createTempFile("comprobante_", ".jpg", folder)
}

/** Lee la imagen, la endereza según la cámara, la reduce a RECEIPT_MAX_SIZE_PX y la comprime en JPEG. */
private fun compressReceipt(context: Context, uri: Uri): ByteArray? {
    val resolver = context.contentResolver
    val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
    resolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, bounds) }
    if (bounds.outWidth <= 0 || bounds.outHeight <= 0) return null

    var sample = 1
    while (maxOf(bounds.outWidth, bounds.outHeight) / (sample * 2) >= RECEIPT_MAX_SIZE_PX) sample *= 2
    val decoded = resolver.openInputStream(uri)?.use {
        BitmapFactory.decodeStream(it, null, BitmapFactory.Options().apply { inSampleSize = sample })
    } ?: return null

    val rotation = resolver.openInputStream(uri)?.use {
        when (ExifInterface(it).getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL)) {
            ExifInterface.ORIENTATION_ROTATE_90 -> 90f
            ExifInterface.ORIENTATION_ROTATE_180 -> 180f
            ExifInterface.ORIENTATION_ROTATE_270 -> 270f
            else -> 0f
        }
    } ?: 0f

    val scale = minOf(1f, RECEIPT_MAX_SIZE_PX.toFloat() / maxOf(decoded.width, decoded.height))
    val matrix = Matrix().apply {
        postScale(scale, scale)
        postRotate(rotation)
    }
    val result = if (scale < 1f || rotation != 0f) {
        Bitmap.createBitmap(decoded, 0, 0, decoded.width, decoded.height, matrix, true).also {
            if (it !== decoded) decoded.recycle()
        }
    } else decoded

    return ByteArrayOutputStream().use { output ->
        result.compress(Bitmap.CompressFormat.JPEG, RECEIPT_JPEG_QUALITY, output)
        result.recycle()
        output.toByteArray()
    }
}
