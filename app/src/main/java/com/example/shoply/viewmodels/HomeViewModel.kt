package com.example.shoply.viewmodels

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.shoply.models.Product
import com.example.shoply.repositories.CartRepository
import com.example.shoply.repositories.FilterCriteria
import com.example.shoply.repositories.HomeRepository
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

data class HomeUiState(
    val products: List<Product> = emptyList(),
    val isLoading: Boolean = false,
    val searchQuery: String = "",
    val filters: FilterCriteria = FilterCriteria(),
    val allCategories: List<String> = emptyList(),
    val allBrands: List<String> = emptyList(),
    val canLoadMore: Boolean = true
)

class HomeViewModel(
    private val homeRepository: HomeRepository,
    private val cartRepository: CartRepository
) : ViewModel() {
    private val _uiState = MutableStateFlow(HomeUiState(isLoading = true))
    val uiState: StateFlow<HomeUiState> = _uiState
    private var lastDocument: com.google.firebase.firestore.DocumentSnapshot? = null
    private var currentPageProducts: List<Product> = emptyList()
    private var searchJob: Job? = null

    init {
        // Load initial data when ViewModel is created
        loadInitialData()
    }

    // Load initial products, categories, and brands
    private fun loadInitialData() {
        viewModelScope.launch {
            try {
                Log.d("HomeViewModel", "Starting to load initial data")
                _uiState.value = _uiState.value.copy(isLoading = true)

                // Fetch products with current filters and search query
                val (products, nextDoc) = homeRepository.getProducts(
                    pageSize = 10,
                    lastDocument = null,
                    searchQuery = _uiState.value.searchQuery.trim().lowercase(),
                    filters = _uiState.value.filters
                )
                currentPageProducts = products
                lastDocument = nextDoc
                Log.d("HomeViewModel", "Loaded ${products.size} initial products, canLoadMore=${nextDoc != null}")

                // Fetch categories and brands
                val categories = try {
                    homeRepository.getAllCategories().also {
                        Log.d("HomeViewModel", "Loaded ${it.size} categories")
                    }
                } catch (e: Exception) {
                    Log.e("HomeViewModel", "Error loading categories: ${e.message}", e)
                    emptyList()
                }

                val brands = try {
                    homeRepository.getAllBrands().also {
                        Log.d("HomeViewModel", "Loaded ${it.size} brands")
                    }
                } catch (e: Exception) {
                    Log.e("HomeViewModel", "Error loading brands: ${e.message}", e)
                    emptyList()
                }

                // Update UI state with fetched data
                _uiState.value = _uiState.value.copy(
                    products = products,
                    isLoading = false,
                    allCategories = categories,
                    allBrands = brands,
                    canLoadMore = nextDoc != null
                )
            } catch (e: Exception) {
                Log.e("HomeViewModel", "Unexpected error loading initial data: ${e.message}", e)
                _uiState.value = _uiState.value.copy(
                    products = emptyList(),
                    isLoading = false,
                    canLoadMore = false
                )
            }
        }
    }

    // Load more products for pagination
    fun loadMoreProducts() {
        viewModelScope.launch {
            try {
                if (!_uiState.value.canLoadMore || _uiState.value.isLoading) {
                    Log.d("HomeViewModel", "Cannot load more products: canLoadMore=${_uiState.value.canLoadMore}, isLoading=${_uiState.value.isLoading}")
                    return@launch
                }
                Log.d("HomeViewModel", "Loading more products")
                _uiState.value = _uiState.value.copy(isLoading = true)

                // Fetch additional products
                val (products, nextDoc) = homeRepository.getProducts(
                    pageSize = 10,
                    lastDocument = lastDocument,
                    searchQuery = _uiState.value.searchQuery.trim().lowercase(),
                    filters = _uiState.value.filters
                )
                currentPageProducts = currentPageProducts + products
                lastDocument = nextDoc
                Log.d("HomeViewModel", "Loaded ${products.size} additional products, total=${currentPageProducts.size}")

                // Update UI state with new products
                _uiState.value = _uiState.value.copy(
                    products = currentPageProducts,
                    isLoading = false,
                    canLoadMore = nextDoc != null
                )
            } catch (e: Exception) {
                Log.e("HomeViewModel", "Error loading more products: ${e.message}", e)
                _uiState.value = _uiState.value.copy(isLoading = false)
            }
        }
    }

    // Update search query with debouncing
    fun updateSearchQuery(query: String) {
        try {
            if (_uiState.value.searchQuery == query) {
                Log.d("HomeViewModel", "Search query unchanged, ignoring: $query")
                return
            }

            searchJob?.cancel()
            searchJob = viewModelScope.launch {
                Log.d("HomeViewModel", "Updating search query to: $query")
                _uiState.value = _uiState.value.copy(searchQuery = query)
                delay(300) // Debounce to avoid rapid searches
                resetAndLoad()
            }
        } catch (e: Exception) {
            Log.e("HomeViewModel", "Error updating search query: ${e.message}", e)
        }
    }

    // Update filter criteria and reload data
    fun updateFilters(
        minPrice: Int? = _uiState.value.filters.minPrice,
        maxPrice: Int? = _uiState.value.filters.maxPrice,
        sortOrder: String = _uiState.value.filters.sortOrder,
        selectedCategories: List<String> = _uiState.value.filters.selectedCategories,
        selectedBrands: List<String> = _uiState.value.filters.selectedBrands
    ) {
        try {
            Log.d("HomeViewModel", "Updating filters: minPrice=$minPrice, maxPrice=$maxPrice, sortOrder=$sortOrder")
            _uiState.value = _uiState.value.copy(
                filters = FilterCriteria(
                    minPrice = minPrice,
                    maxPrice = maxPrice,
                    sortOrder = sortOrder,
                    selectedCategories = selectedCategories,
                    selectedBrands = selectedBrands
                )
            )
            resetAndLoad()
        } catch (e: Exception) {
            Log.e("HomeViewModel", "Error updating filters: ${e.message}", e)
        }
    }

    // Reset pagination and reload data
    private fun resetAndLoad() {
        try {
            Log.d("HomeViewModel", "Resetting pagination and reloading data")
            currentPageProducts = emptyList()
            lastDocument = null
            loadInitialData()
        } catch (e: Exception) {
            Log.e("HomeViewModel", "Error resetting and loading data: ${e.message}", e)
        }
    }

    // Add a product to the cart
    suspend fun addToCart(product: Product): Boolean {
        return try {
            val result = cartRepository.addToCart(product)
            result.onSuccess {
                Log.d("HomeViewModel", "Successfully added product to cart: ${product.name}")
            }.onFailure { exception ->
                Log.e("HomeViewModel", "Failed to add product to cart: ${exception.message}", exception)
            }
            result.isSuccess
        } catch (e: Exception) {
            Log.e("HomeViewModel", "Unexpected error adding to cart: ${e.message}", e)
            false
        }
    }
}