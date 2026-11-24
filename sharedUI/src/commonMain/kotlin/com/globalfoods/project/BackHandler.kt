package com.globalfoods.project

import androidx.compose.runtime.Composable

/**
 * Atiende el botón o gesto "atrás" del sistema mientras [enabled] sea true, para regresar a la
 * pantalla anterior de la app en lugar de cerrarla.
 */
@Composable
internal expect fun PlatformBackHandler(enabled: Boolean = true, onBack: () -> Unit)
