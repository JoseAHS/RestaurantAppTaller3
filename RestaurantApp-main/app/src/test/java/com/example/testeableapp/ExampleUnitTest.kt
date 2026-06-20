package com.example.testeableapp

import com.example.testeableapp.model.MenuData
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.Assert.*

/**
 * Pruebas unitarias de RestaurantViewModel.
 *
 * Se usa un StandardTestDispatcher como Main dispatcher porque el ViewModel
 * usa viewModelScope para convertir los Flow en StateFlow (stateIn). Además,
 * los StateFlow derivados (orderedItems, total, isEmpty, uiState) usan
 * SharingStarted.WhileSubscribed(5000): solo se mantienen "activos" y se
 * actualizan mientras exista al menos un colector suscrito. Por eso, en cada
 * test se lanza una colección de fondo (backgroundScope) sobre esos flujos
 * antes de hacer las aserciones; sin esto, los valores quedarían "congelados"
 * en su estado inicial (0.0, listas vacías, etc.) aunque _quantities cambie.
 */
class ExampleUnitTest {

    private val testDispatcher = StandardTestDispatcher()
    private lateinit var viewModel: RestaurantViewModel

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        viewModel = RestaurantViewModel()
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    /**
     * Activa la colección de los StateFlow derivados del ViewModel para que
     * se actualicen durante el test (ver explicación en el comentario de
     * la clase). Debe llamarse al inicio de cada test que lea total,
     * orderedItems, isEmpty o confirmation.
     */
    private fun TestScope.activarColectoresDeEstado() {
        viewModel.orderedItems.onEach { }.launchIn(backgroundScope)
        viewModel.total.onEach { }.launchIn(backgroundScope)
        viewModel.isEmpty.onEach { }.launchIn(backgroundScope)
        viewModel.confirmation.onEach { }.launchIn(backgroundScope)
    }

    // ------------------------------------------------------------------
    // 1. Agregar item al pedido (0.5 pts)
    // ------------------------------------------------------------------
    @Test
    fun `agregar item agrega el producto al pedido con cantidad 1`() = runTest {
        activarColectoresDeEstado()
        val item = MenuData.items.first() // Patatas Bravas, id = 1

        viewModel.addItem(item.id)
        testDispatcher.scheduler.advanceUntilIdle()

        val quantities = viewModel.quantities.value
        val orderedItems = viewModel.orderedItems.value

        assertEquals(1, quantities[item.id])
        assertTrue(orderedItems.any { it.id == item.id })
        assertFalse(viewModel.isEmpty.value)
    }

    // ------------------------------------------------------------------
    // 2. Incrementar / Decrementar cantidad (0.5 pts)
    // ------------------------------------------------------------------
    @Test
    fun `incrementItem aumenta la cantidad del producto en 1`() = runTest {
        val item = MenuData.items.first()

        viewModel.addItem(item.id) // cantidad = 1
        viewModel.incrementItem(item.id) // cantidad = 2
        testDispatcher.scheduler.advanceUntilIdle()

        assertEquals(2, viewModel.quantities.value[item.id])
    }

    @Test
    fun `decrementItem disminuye la cantidad del producto en 1`() = runTest {
        val item = MenuData.items.first()

        viewModel.addItem(item.id) // cantidad = 1
        viewModel.incrementItem(item.id) // cantidad = 2
        viewModel.decrementItem(item.id) // cantidad = 1
        testDispatcher.scheduler.advanceUntilIdle()

        assertEquals(1, viewModel.quantities.value[item.id])
    }

    // ------------------------------------------------------------------
    // 3. Eliminar item al decrementar desde 1 (0.5 pts)
    // ------------------------------------------------------------------
    @Test
    fun `decrementItem elimina el producto del pedido cuando la cantidad llega a 0`() = runTest {
        activarColectoresDeEstado()
        val item = MenuData.items.first()

        viewModel.addItem(item.id) // cantidad = 1
        viewModel.decrementItem(item.id) // debería eliminarlo del mapa
        testDispatcher.scheduler.advanceUntilIdle()

        val quantities = viewModel.quantities.value
        val orderedItems = viewModel.orderedItems.value

        assertFalse(quantities.containsKey(item.id))
        assertFalse(orderedItems.any { it.id == item.id })
        assertTrue(viewModel.isEmpty.value)
    }

    // ------------------------------------------------------------------
    // 4. Cálculo del total a pagar (0.5 pts)
    // ------------------------------------------------------------------
    @Test
    fun `total calcula correctamente la suma de precio por cantidad de varios items`() = runTest {
        activarColectoresDeEstado()
        val patatas = MenuData.items.first { it.name == "Patatas Bravas" } // 5.50
        val cerveza = MenuData.items.first { it.name == "Cerveza" }        // 3.00

        viewModel.addItem(patatas.id)       // 1 x 5.50 = 5.50
        viewModel.incrementItem(patatas.id) // 2 x 5.50 = 11.00
        viewModel.addItem(cerveza.id)       // 1 x 3.00 = 3.00
        testDispatcher.scheduler.advanceUntilIdle()

        val totalEsperado = (2 * patatas.price) + (1 * cerveza.price) // 14.00
        assertEquals(totalEsperado, viewModel.total.value, 0.001)
    }

    // ==================================================================
    // PRUEBAS UNITARIAS ADICIONALES (análisis de lógica interna)
    // ==================================================================

    /**
     * Aspecto adicional #1: decrementItem sobre un item que NO está en el
     * pedido (cantidad 0 / inexistente).
     *
     * Justificación: el código usa `current[itemId] ?: return` dentro del
     * bloque `_quantities.update { ... }`. En Kotlin, un `return` sin
     * etiqueta dentro de una lambda pasada a una función inline (como
     * `update`) es un "non-local return": no solo termina el lambda, sino
     * que termina toda la función decrementItem() sin lanzar excepción y
     * sin modificar el estado. Es importante validar que la app no se
     * rompe (no crashea) si la UI llegara a invocar decrementItem sobre un
     * producto que ya no está en el pedido (p. ej. por un doble clic rápido
     * en el botón "-" justo cuando el item es eliminado), y que el estado
     * permanece intacto en ese caso.
     */
    @Test
    fun `decrementItem sobre un item que no esta en el pedido no lanza excepcion y no modifica el estado`() = runTest {
        activarColectoresDeEstado()
        val item = MenuData.items.first()
        val estadoPrevio = viewModel.quantities.value

        // El item nunca fue agregado: no debería lanzar excepción.
        viewModel.decrementItem(item.id)
        testDispatcher.scheduler.advanceUntilIdle()

        assertEquals(estadoPrevio, viewModel.quantities.value)
        assertTrue(viewModel.isEmpty.value)
    }

    /**
     * Aspecto adicional #2: placeOrder() con el pedido vacío.
     *
     * Justificación: placeOrder() tiene un guard `if (items.isEmpty()) return`.
     * Es relevante testear que, si por alguna razón se invoca placeOrder()
     * sin productos en el pedido (la UI no debería permitirlo, pero la
     * función pública del ViewModel sí podría ser llamada en ese estado),
     * no se genera una confirmación falsa con itemCount = 0. Esto evita
     * mostrar al usuario un diálogo de "pedido confirmado" sin productos.
     */
    @Test
    fun `placeOrder con el pedido vacio no genera una confirmacion`() = runTest {
        activarColectoresDeEstado()
        viewModel.placeOrder()
        testDispatcher.scheduler.advanceUntilIdle()

        assertNull(viewModel.confirmation.value)
    }

    /**
     * Prueba complementaria: placeOrder() con productos sí genera la
     * confirmación correcta (cantidad total de artículos y total a pagar).
     */
    @Test
    fun `placeOrder con productos genera confirmacion con itemCount y total correctos`() = runTest {
        activarColectoresDeEstado()
        val patatas = MenuData.items.first { it.name == "Patatas Bravas" } // 5.50
        val cerveza = MenuData.items.first { it.name == "Cerveza" }        // 3.00

        viewModel.addItem(patatas.id)
        viewModel.incrementItem(patatas.id) // 2 patatas
        viewModel.addItem(cerveza.id)       // 1 cerveza
        testDispatcher.scheduler.advanceUntilIdle()

        viewModel.placeOrder()
        testDispatcher.scheduler.advanceUntilIdle()

        val confirmacion = viewModel.confirmation.value
        assertNotNull(confirmacion)
        assertEquals(3, confirmacion!!.itemCount) // 2 patatas + 1 cerveza
        assertEquals((2 * patatas.price) + cerveza.price, confirmacion.total, 0.001)
    }

    /**
     * Prueba complementaria: dismissConfirmation() limpia tanto la
     * confirmación como el pedido (vuelve a estado vacío para un nuevo pedido).
     */
    @Test
    fun `dismissConfirmation limpia la confirmacion y reinicia el pedido`() = runTest {
        activarColectoresDeEstado()
        val item = MenuData.items.first()
        viewModel.addItem(item.id)
        viewModel.placeOrder()
        testDispatcher.scheduler.advanceUntilIdle()

        viewModel.dismissConfirmation()
        testDispatcher.scheduler.advanceUntilIdle()

        assertNull(viewModel.confirmation.value)
        assertTrue(viewModel.quantities.value.isEmpty())
        assertTrue(viewModel.isEmpty.value)
    }
}
