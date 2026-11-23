package com.globalfoods.project.sharedlogic.network.core

import com.globalfoods.project.sharedlogic.network.auth.UserRole

/*
 * Datos de la sesión actual. Están en su propio archivo, separados de globalFoodsClient, para que
 * leer el rol (por ejemplo, en una regla de negocio) no cree el cliente HTTP. Así las pruebas
 * unitarias, que no tienen motor HTTP, pueden usar el rol sin fallar.
 */

var sessionToken: String? = null
var sessionNombreUsuario: String? = null
var sessionRol: UserRole = UserRole.Administrator
/** IdCliente del usuario con rol Cliente; null mientras la API no lo devuelva en el login. */
var sessionIdCliente: Int? = null
