package com.globalfoods.project

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

/*
 * Foto del comprobante de entrega. Cada plataforma abre su cámara o su galería y devuelve la
 * imagen ya reducida y comprimida en JPEG, lista para mandarla a la API cuando tenga dónde guardarla.
 */

/** Lado más largo, en píxeles, con el que se guarda la foto. */
internal const val RECEIPT_MAX_SIZE_PX = 1280

/** Calidad JPEG (0 a 100) de la foto guardada: buena para leer un recibo y ligera para enviarla. */
internal const val RECEIPT_JPEG_QUALITY = 75

/** Abre la cámara o la galería para elegir la foto del comprobante. */
internal interface ReceiptPhotoPicker {
    fun takePhoto()
    fun pickFromGallery()
}

/**
 * [onPhoto] recibe la foto en JPEG; [onError] recibe un mensaje para mostrar si no se pudo leer
 * o si el teléfono no tiene cámara. Si el usuario cancela, no se llama a ninguno.
 */
@Composable
internal expect fun rememberReceiptPhotoPicker(
    onPhoto: (ByteArray) -> Unit,
    onError: (String) -> Unit
): ReceiptPhotoPicker

/** Miniatura de la foto (JPEG) que se tomó. */
@Composable
internal expect fun ReceiptPhotoPreview(jpeg: ByteArray, modifier: Modifier = Modifier)
