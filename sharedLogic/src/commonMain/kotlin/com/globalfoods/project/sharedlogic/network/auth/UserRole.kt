package com.globalfoods.project.sharedlogic.network.auth

/** Rol con el que inició sesión el usuario (campo tipoUsuario del login). */
enum class UserRole {
    Administrator,
    Employee,
    Reports,
    Client;

    /** Vendedores (empleados) y administradores registran el método de pago de un pedido entregado. */
    val canRegisterPayment: Boolean get() = this == Administrator || this == Employee

    companion object {
        /**
         * Igual que la app web: compara sin importar mayúsculas y, si la API no manda el rol,
         * se trata como Administrador.
         */
        fun fromApi(value: String?): UserRole = when (value?.trim()?.lowercase()) {
            "empleado" -> Employee
            "reporte", "reportes" -> Reports
            "cliente" -> Client
            else -> Administrator
        }
    }
}
