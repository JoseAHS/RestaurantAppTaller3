package com.example.testeableapp

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.testeableapp.model.MenuData
import com.example.testeableapp.model.MenuItem
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update

data class OrderConfirmation(
    val itemCount: Int,
    val total: Double
)

data class OrderUiState(
    val quantities: Map<Int, Int> = emptyMap(),
    val orderedItems: List<MenuItem> = emptyList(),
    val total: Double = 0.0,
    val isEmpty: Boolean = true,
    val confirmation: OrderConfirmation? = null
)

class RestaurantViewModel : ViewModel() {

    private val _quantities = MutableStateFlow<Map<Int, Int>>(emptyMap())
    val quantities: StateFlow<Map<Int, Int>> = _quantities.asStateFlow()

    val orderedItems: StateFlow<List<MenuItem>> = _quantities
        .map { q -> MenuData.items.filter { (q[it.id] ?: 0) > 0 } }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val total: StateFlow<Double> = _quantities
        .map { q -> q.entries.sumOf { (id, qty) -> (MenuData.items.find { it.id == id }?.price ?: 0.0) * qty } }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0.0)

    val isEmpty: StateFlow<Boolean> = _quantities
        .map { it.isEmpty() }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), true)

    private val _confirmation = MutableStateFlow<OrderConfirmation?>(null)
    val confirmation: StateFlow<OrderConfirmation?> = _confirmation.asStateFlow()

    val uiState: StateFlow<OrderUiState> = combine(
        _quantities, orderedItems, total, isEmpty, _confirmation
    ) { quantities, orderedItems, total, isEmpty, confirmation ->
        OrderUiState(
            quantities = quantities,
            orderedItems = orderedItems,
            total = total,
            isEmpty = isEmpty,
            confirmation = confirmation
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), OrderUiState())

    fun addItem(itemId: Int) {
        _quantities.update { current ->
            current + (itemId to ((current[itemId] ?: 0) + 1))
        }
    }

    fun incrementItem(itemId: Int) {
        _quantities.update { current ->
            current + (itemId to ((current[itemId] ?: 0) + 1))
        }
    }

    fun decrementItem(itemId: Int) {
        _quantities.update { current ->
            val currentQty = current[itemId] ?: return
            if (currentQty <= 1) current - itemId
            else current + (itemId to (currentQty - 1))
        }
    }

    fun placeOrder() {
        val items = orderedItems.value
        val q = _quantities.value
        if (items.isEmpty()) return
        _confirmation.value = OrderConfirmation(
            itemCount = items.sumOf { q[it.id] ?: 0 },
            total = total.value
        )
    }

    fun dismissConfirmation() {
        _confirmation.value = null
        _quantities.value = emptyMap()
    }
}

