package com.globalfoods.project

import com.globalfoods.project.sharedlogic.network.auth.UserRole
import com.globalfoods.project.sharedlogic.network.dashboard.ArticuloInventario
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class AddProductsTest {
    private val inventory = listOf(
        ArticuloInventario(codigoProducto = 101, talla = "26-30", precio = 134.0, master = 0.0, cantidadKg = 40.0),
        ArticuloInventario(codigoProducto = 102, talla = "26/30 PREMIUM", precio = 134.0, master = 0.0, cantidadKg = 20.0),
        ArticuloInventario(codigoProducto = 205, talla = "31-35", precio = 127.0, master = 0.0, cantidadKg = 0.5),
        ArticuloInventario(codigoProducto = 300, talla = "16/20", precio = 180.0, master = 0.0, cantidadKg = 0.0)
    )

    @Test
    fun searchesBySizeOrCode() {
        assertEquals(listOf("26-30", "26/30 PREMIUM"), searchInventory(inventory, "26").map { it.talla })
        assertEquals(listOf("26/30 PREMIUM"), searchInventory(inventory, "premium").map { it.talla })
        assertEquals(listOf("31-35"), searchInventory(inventory, "205").map { it.talla })
        assertEquals(inventory, searchInventory(inventory, "  "))
    }

    @Test
    fun addedProductsKeepTheOrderInWhichTheyWereAdded() {
        val quantities = linkedMapOf("31-35" to "0.5", "26-30" to "4")
        assertEquals(listOf("31-35", "26-30"), addedProducts(quantities, inventory).map { it.talla })
    }

    @Test
    fun startsWithOneKgOrWhatIsAvailable() {
        assertEquals("1", initialQuantity(inventory[0], UserRole.Seller))
        assertEquals("0.5", initialQuantity(inventory[2], UserRole.Seller))
        // El cliente no ve existencias: siempre empieza en 1 kg.
        assertEquals("1", initialQuantity(inventory[2], UserRole.Client))
    }

    @Test
    fun stepperStaysBetweenOneAndTheAvailableKg() {
        assertEquals("5", stepQuantity("4", 1.0, 40.0))
        assertEquals("40", stepQuantity("39.5", 1.0, 40.0))
        assertEquals("1", stepQuantity("1.5", -1.0, 40.0))
        assertEquals("3", stepQuantity("4", -1.0, 40.0))
    }

    @Test
    fun marksZeroAndOverStockOnTheirOwnLine() {
        val quantities = mapOf("26-30" to "45", "26/30 PREMIUM" to "0", "31-35" to "")
        assertEquals(
            mapOf("26/30 PREMIUM" to QUANTITY_MESSAGE, "31-35" to QUANTITY_MESSAGE),
            zeroQuantityErrors(quantities)
        )
        assertEquals(
            mapOf("26-30" to "Solo hay 40 kg disponibles de 26-30. Ajuste la cantidad."),
            stockErrors(quantities, inventory)
        )
        assertTrue(stockErrors(mapOf("26-30" to "40"), inventory).isEmpty())
    }

    @Test
    fun formatsMoneyWithTwoDecimals() {
        assertEquals("$670.00", formatMoney(670.0))
        assertEquals("$1,234.50", formatMoney(1234.5))
        assertEquals("$0.00", formatMoney(0.0))
    }
}
