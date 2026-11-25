package com.globalfoods.project.sharedlogic.network.auth

/** Rol con el que inició sesión el usuario (campo tipoUsuario del login). */
enum class UserRole {
    Administrator,
    Seller,
    Client;

    /** Vendedores y administradores registran el método de pago durante el reparto. */
    val canRegisterPayment: Boolean get() = this != Client

    /** El cliente no ve existencias: ni la tabla del dashboard ni los kilos disponibles del catálogo. */
    val canSeeInventory: Boolean get() = this != Client

    /** El cliente solo agrega y consulta sus pedidos. */
    val canEditOrders: Boolean get() = this != Client
    /** Cancelar es una baja lógica (Estatus 5); los pedidos nunca se borran. */
    val canCancelOrders: Boolean get() = this != Client

    /**
     * Alta, edición y baja de clientes y de sus sucursales: solo personal administrativo
     * (RN-18, CU-16 a CU-22). El vendedor (asesora) solo los consulta (CU-03).
     */
    val canManageClients: Boolean get() = this == Administrator

    /** El cliente administra sus propias sucursales desde su pestaña Sucursales. */
    val canManageOwnBranches: Boolean get() = this == Client

    companion object {
        /** Se muestra en el login cuando la API manda un rol que la app no maneja. */
        const val INVALID_ROLE_MESSAGE = "Tu usuario no tiene un rol válido. Comunícate con el administrador."

        /**
         * Compara sin importar mayúsculas ni espacios. Devuelve null si la API no manda el rol o manda
         * uno que la app no maneja (por ejemplo, Reportes); en ese caso no se permite iniciar sesión.
         */
        fun fromApi(value: String?): UserRole? = when (value?.trim()?.lowercase()) {
            "administrador", "admin", "administrator" -> Administrator
            "vendedor", "empleado" -> Seller
            "cliente" -> Client
            else -> null
        }
    }
}
