package com.example.testeableapp.model

data class MenuItem(
    val id: Int,
    val name: String,
    val price: Double,
    val category: String
)

object MenuData {
    val items = listOf(
        MenuItem(1, "Patatas Bravas", 5.50, "Tapas"),
        MenuItem(2, "Croquetas (6 uds)", 6.00, "Tapas"),
        MenuItem(3, "Calamares", 7.50, "Tapas"),
        MenuItem(4, "Tortilla Española", 4.50, "Tapas"),
        MenuItem(5, "Agua mineral", 1.50, "Bebidas"),
        MenuItem(6, "Refresco", 2.00, "Bebidas"),
        MenuItem(7, "Cerveza", 3.00, "Bebidas"),
        MenuItem(8, "Flan de huevo", 4.00, "Postres"),
        MenuItem(9, "Helado", 3.50, "Postres"),
        MenuItem(10, "Tarta de queso", 5.00, "Postres"),
    )

    val categories = items.map { it.category }.distinct()
}
