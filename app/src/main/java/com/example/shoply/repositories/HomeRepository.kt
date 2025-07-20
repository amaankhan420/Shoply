package com.example.shoply.repositories

import android.util.Log
import com.example.shoply.models.Product
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import kotlinx.coroutines.tasks.await

class HomeRepository(
    private val firestore: FirebaseFirestore
) {
    // Fetch products with pagination, search, and filtering
    suspend fun getProducts(
        pageSize: Int = 10,
        lastDocument: com.google.firebase.firestore.DocumentSnapshot? = null,
        searchQuery: String = "",
        filters: FilterCriteria = FilterCriteria()
    ): Pair<List<Product>, com.google.firebase.firestore.DocumentSnapshot?> {
        return try {
            var query: Query = firestore.collection("products")
            Log.d(
                "HomeRepository",
                "Starting product query with pageSize=$pageSize, searchQuery='$searchQuery'"
            )

            // Apply search query filter
            if (searchQuery.isNotEmpty()) {
                query = query.whereArrayContains("searchKeywords", searchQuery)
                Log.d("HomeRepository", "Applied search filter: $searchQuery")
            }

            // Apply category filter
            if (filters.selectedCategories.isNotEmpty()) {
                query = query.whereIn("category", filters.selectedCategories)
                Log.d("HomeRepository", "Applied category filter: ${filters.selectedCategories}")
            }

            // Apply brand filter
            if (filters.selectedBrands.isNotEmpty()) {
                query = query.whereIn("brand", filters.selectedBrands)
                Log.d("HomeRepository", "Applied brand filter: ${filters.selectedBrands}")
            }

            // Apply minimum price filter
            if (filters.minPrice != null) {
                query = query.whereGreaterThanOrEqualTo("price", filters.minPrice)
                Log.d("HomeRepository", "Applied min price filter: ${filters.minPrice}")
            }

            // Apply maximum price filter
            if (filters.maxPrice != null) {
                query = query.whereLessThanOrEqualTo("price", filters.maxPrice)
                Log.d("HomeRepository", "Applied max price filter: ${filters.maxPrice}")
            }

            // Handle pagination
            if (lastDocument != null) {
                query = query.startAfter(lastDocument)
                Log.d("HomeRepository", "Applied pagination with last document")
            }

            // Limit the number of results
            query = query.limit(pageSize.toLong())
            Log.d("HomeRepository", "Set query limit to $pageSize")

            // Execute the query
            val snapshot = query.get().await()
            Log.d("HomeRepository", "Retrieved ${snapshot.documents.size} products from Firestore")

            // Map Firestore documents to Product objects
            val products = snapshot.documents.mapNotNull { doc ->
                val data = doc.data ?: return@mapNotNull null
                try {
                    Product(
                        id = doc.id,
                        name = data["name"] as? String ?: "",
                        price = (data["price"] as? Number)?.toInt() ?: 0,
                        image = data["image"] as? String ?: "",
                        brand = data["brand"] as? String ?: "",
                        category = data["category"] as? String ?: ""
                    )
                } catch (e: Exception) {
                    Log.e("HomeRepository", "Error mapping document ${doc.id}: ${e.message}", e)
                    null
                }
            }

            // Apply sorting based on filter criteria
            val sortedProducts = when (filters.sortOrder) {
                "low_to_high" -> {
                    Log.d("HomeRepository", "Sorting products by price: low to high")
                    products.sortedBy { it.price }
                }

                "high_to_low" -> {
                    Log.d("HomeRepository", "Sorting products by price: high to low")
                    products.sortedByDescending { it.price }
                }

                else -> {
                    Log.d("HomeRepository", "No sorting applied")
                    products
                }
            }

            val nextDocument = snapshot.documents.lastOrNull()
            Log.d(
                "HomeRepository",
                "Returning ${sortedProducts.size} products with nextDocument=${nextDocument != null}"
            )
            sortedProducts to nextDocument

        } catch (e: Exception) {
            Log.e("HomeRepository", "Failed to fetch products: ${e.message}", e)
            emptyList<Product>() to null
        }
    }

    // Fetch all unique product categories
    suspend fun getAllCategories(): List<String> {
        return try {
            val snapshot = firestore.collection("products").get().await()
            val categories = snapshot.documents.mapNotNull { it.getString("category") }.distinct()
            Log.d("HomeRepository", "Retrieved ${categories.size} unique categories")
            categories
        } catch (e: Exception) {
            Log.e("HomeRepository", "Failed to retrieve categories: ${e.message}", e)
            emptyList()
        }
    }

    // Fetch all unique product brands
    suspend fun getAllBrands(): List<String> {
        return try {
            val snapshot = firestore.collection("products").get().await()
            val brands = snapshot.documents.mapNotNull { it.getString("brand") }.distinct()
            Log.d("HomeRepository", "Retrieved ${brands.size} unique brands")
            brands
        } catch (e: Exception) {
            Log.e("HomeRepository", "Failed to retrieve brands: ${e.message}", e)
            emptyList()
        }
    }
}

data class FilterCriteria(
    val minPrice: Int? = null,
    val maxPrice: Int? = null,
    val sortOrder: String = "none",
    val selectedCategories: List<String> = emptyList(),
    val selectedBrands: List<String> = emptyList()
)