package com.example.testeableapp

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertTextEquals
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import com.example.testeableapp.model.MenuData
import org.junit.Before
import org.junit.Rule
import org.junit.Test

/**
 * Pruebas de UI (Compose Testing / instrumented tests) para la pantalla
 * principal de RestaurantOrderApp (MainActivity).
 */
class RestaurantOrderTest {

    @get:Rule
    val composeTestRule = createAndroidComposeRule<MainActivity>()

    /**
     * createAndroidComposeRule reutiliza la misma Activity (y por lo tanto
     * el mismo RestaurantViewModel) entre los distintos @Test de esta clase
     * para ahorrar tiempo de ejecución. Esto significa que el pedido de un
     * test puede quedar "contaminando" el estado inicial del siguiente test.
     *
     * Para garantizar que cada test arranque siempre con el pedido vacío,
     * antes de cada prueba se revisa el estado en el que quedó la pantalla
     * y se limpia en bucle hasta volver a "pedido vacío":
     * - Si quedó el diálogo de confirmación abierto, se acepta para
     *   cerrarlo (dispara dismissConfirmation() en el ViewModel).
     * - Si quedó un pedido sin confirmar (botón "Realizar Pedido" visible),
     *   se completa y se descarta (placeOrder + aceptar el diálogo).
     * Tras cada click se llama a waitForIdle() para forzar que Compose
     * recomponga con el nuevo valor del StateFlow antes de seguir, ya que
     * el ViewModel actualiza el estado a través de corrutinas y
     * viewModelScope, y sin esta espera explícita una acción podía
     * ejecutarse antes de que la recomposición anterior terminara.
     */
    @Before
    fun resetearPedidoSiQuedoAlgoDeUnTestAnterior() {
        composeTestRule.waitForIdle()

        repeat(3) {
            val hayDialogoAbierto = composeTestRule
                .onAllNodesWithTag("confirmationDialog")
                .fetchSemanticsNodes()
                .isNotEmpty()

            if (hayDialogoAbierto) {
                composeTestRule.onNodeWithTag("confirmationOkButton").performClick()
                composeTestRule.waitForIdle()
                return@repeat
            }

            val hayBotonDePedido = composeTestRule
                .onAllNodesWithTag("placeOrderButton")
                .fetchSemanticsNodes()
                .isNotEmpty()

            if (hayBotonDePedido) {
                composeTestRule.onNodeWithTag("placeOrderButton").performScrollTo().performClick()
                composeTestRule.waitForIdle()
                composeTestRule.onNodeWithTag("confirmationOkButton").performClick()
                composeTestRule.waitForIdle()
            }
        }
    }

    // ------------------------------------------------------------------
    // 1. Mensaje de pedido vacío visible al inicio (1 pt)
    // ------------------------------------------------------------------
    @Test
    fun mensajeDePedidoVacio_esVisibleAlInicio() {
        composeTestRule.onNodeWithTag("emptyOrderMessage")
            .assertIsDisplayed()
            .assertTextEquals("El pedido está vacío. Añade productos del menú.")
    }

    // ------------------------------------------------------------------
    // 2. Todos los items del menú visibles (1 pt)
    // ------------------------------------------------------------------
    @Test
    fun todosLosItemsDelMenu_sonVisibles() {
        MenuData.items.forEach { item ->
            composeTestRule.onNodeWithTag("menuItem_${item.id}")
                .assertIsDisplayed()

            composeTestRule.onNodeWithTag("menuItemName_${item.id}")
                .assertIsDisplayed()
                .assertTextEquals(item.name)
        }
    }

    // ------------------------------------------------------------------
    // 3. El total general se actualiza (2 pts)
    // ------------------------------------------------------------------
    @Test
    fun totalGeneral_seActualizaAlAgregarProductos() {
        val patatas = MenuData.items.first { it.name == "Patatas Bravas" } // 5.50
        val cerveza = MenuData.items.first { it.name == "Cerveza" }        // 3.00

        // Agregar una Patata Brava
        composeTestRule.onNodeWithTag("addButton_${patatas.id}").performClick()
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithTag("totalValue")
            .assertTextEquals("%.2f €".format(patatas.price))

        // Agregar una Cerveza adicional y verificar que el total suma ambos
        composeTestRule.onNodeWithTag("addButton_${cerveza.id}").performClick()
        composeTestRule.waitForIdle()

        val totalEsperado = "%.2f €".format(patatas.price + cerveza.price)
        composeTestRule.onNodeWithTag("totalValue")
            .assertTextEquals(totalEsperado)

        // Incrementar la cantidad de patatas desde la sección "Tu Pedido"
        composeTestRule.onNodeWithTag("incrementOrderItem_${patatas.id}").performClick()
        composeTestRule.waitForIdle()

        val totalEsperado2 = "%.2f €".format((2 * patatas.price) + cerveza.price)
        composeTestRule.onNodeWithTag("totalValue")
            .assertTextEquals(totalEsperado2)
    }

    // ==================================================================
    // PRUEBAS DE UI ADICIONALES
    // ==================================================================

    /**
     * Aspecto adicional #1: el mensaje de "pedido vacío" debe DEJAR de
     * mostrarse en cuanto se agrega un producto, y el botón "Realizar
     * Pedido" debe aparecer.
     *
     * Justificación: la regla de evaluación original solo pide comprobar
     * que el mensaje vacío se vea al inicio, pero no que desaparezca al
     * dejar de estar vacío el pedido. Esto es relevante porque la lógica
     * de la UI (if isEmpty / else) depende de un solo booleano: si por un
     * error de estado el mensaje vacío y la lista de productos pudieran
     * coexistir, el usuario vería una pantalla confusa o contradictoria.
     */
    @Test
    fun alAgregarProducto_elMensajeVacioDesapareceYApareceElBotonDePedido() {
        // Estado inicial: mensaje vacío visible, botón de pedido no existe.
        composeTestRule.onNodeWithTag("emptyOrderMessage").assertIsDisplayed()
        composeTestRule.onNodeWithTag("placeOrderButton").assertDoesNotExist()

        val item = MenuData.items.first()
        composeTestRule.onNodeWithTag("addButton_${item.id}").performClick()
        composeTestRule.waitForIdle()

        // Tras agregar un producto, el mensaje vacío ya no debe existir
        // en el árbol de UI, y el botón de "Realizar Pedido" sí debe existir.
        composeTestRule.onNodeWithTag("emptyOrderMessage").assertDoesNotExist()
        composeTestRule.onNodeWithTag("placeOrderButton").assertIsDisplayed()
    }

    /**
     * Aspecto adicional #2: al pulsar "Realizar Pedido" debe aparecer el
     * diálogo de confirmación con el mensaje correcto (cantidad de
     * artículos y total), y al aceptar, el pedido debe reiniciarse
     * (vuelve a mostrarse el mensaje de pedido vacío).
     *
     * Justificación: este es el flujo de checkout completo de la app; es
     * el punto donde más valor de negocio se concentra (confirmar el
     * pedido del cliente) y donde un fallo sería más costoso (p. ej. un
     * pedido que no se reinicia y se "acumula" con el siguiente cliente,
     * o un diálogo que muestra cifras incorrectas).
     */
    @Test
    fun flujoCompletoDeConfirmacionDePedido_muestraDialogoYReinicia() {
        val item = MenuData.items.first { it.name == "Cerveza" } // 3.00

        composeTestRule.onNodeWithTag("addButton_${item.id}").performClick()
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag("incrementOrderItem_${item.id}").performClick() // cantidad = 2
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithTag("placeOrderButton").performScrollTo().performClick()
        composeTestRule.waitForIdle()

        // El diálogo de confirmación debe mostrarse con el mensaje correcto.
        val totalEsperado = "%.2f €".format(2 * item.price)
        composeTestRule.onNodeWithTag("confirmationDialog").assertIsDisplayed()
        composeTestRule.onNodeWithTag("confirmationMessage")
            .assertTextEquals(
                "¡Pedido de 2 artículos por un total de $totalEsperado recibido! Preparen los fogones."
            )

        // Al aceptar, el diálogo se cierra y el pedido vuelve a estar vacío.
        composeTestRule.onNodeWithTag("confirmationOkButton").performClick()
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithTag("confirmationDialog").assertDoesNotExist()
        composeTestRule.onNodeWithTag("emptyOrderMessage").assertIsDisplayed()
    }
}
