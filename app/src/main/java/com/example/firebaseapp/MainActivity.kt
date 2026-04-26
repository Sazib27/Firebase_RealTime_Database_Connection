package com.example.firebaseapp

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.google.firebase.database.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.text.input.KeyboardType
import com.example.firebaseapp.ui.theme.FirebaseappTheme

class MainActivity : ComponentActivity() {

    private lateinit var database: DatabaseReference

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        database = FirebaseDatabase.getInstance().reference.child("products")

        setContent {
            FirebaseappTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    ProductApp(database)
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)   // ← THIS FIXES THE WARNING
@Composable
fun ProductApp(database: DatabaseReference) {
    var productName by remember { mutableStateOf("") }
    var productPrice by remember { mutableStateOf("") }
    var productCategory by remember { mutableStateOf("") }
    var showProducts by remember { mutableStateOf(false) }
    var productList by remember { mutableStateOf<Map<String, Product>>(emptyMap()) }
    var statusMessage by remember { mutableStateOf("") }

    val categories = listOf("Appetizer", "Main Course", "Dessert", "Beverage", "Fast Food")
    var expanded by remember { mutableStateOf(false) }

    fun sanitizeKey(name: String): String {
        return name
            .replace(".", "_")
            .replace("$", "_")
            .replace("#", "_")
            .replace("[", "_")
            .replace("]", "_")
            .replace("/", "_")
    }

    fun addProduct(name: String, price: String, category: String) {
        if (name.isBlank() || price.isBlank() || category.isBlank()) {
            statusMessage = "Please fill all fields"
            return
        }
        val safeKey = sanitizeKey(name)
        val product = Product(productPrice = price, productCategory = category)
        database.child(safeKey).setValue(product)
            .addOnSuccessListener {
                statusMessage = "Product added successfully!"
                productName = ""
                productPrice = ""
                productCategory = ""
            }
            .addOnFailureListener { e ->
                statusMessage = "Error: ${e.message}"
            }
    }

    fun fetchProducts() {
        database.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val map = mutableMapOf<String, Product>()
                for (child in snapshot.children) {
                    val product = child.getValue(Product::class.java)
                    if (product != null) {
                        map[child.key ?: continue] = product
                    }
                }
                productList = map
                statusMessage = if (map.isEmpty()) "No products found" else "${map.size} products loaded"
            }

            override fun onCancelled(error: DatabaseError) {
                statusMessage = "Error loading products: ${error.message}"
            }
        })
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "Restaurant Food Delivery App",
            fontSize = 24.sp,
            style = MaterialTheme.typography.headlineMedium,
            modifier = Modifier.padding(bottom = 32.dp)
        )

        OutlinedTextField(
            value = productName,
            onValueChange = { productName = it },
            label = { Text("Product Name") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true
        )

        Spacer(modifier = Modifier.height(8.dp))

        OutlinedTextField(
            value = productPrice,
            onValueChange = { productPrice = it },
            label = { Text("Product Price") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal)
        )

        Spacer(modifier = Modifier.height(8.dp))

        ExposedDropdownMenuBox(
            expanded = expanded,
            onExpandedChange = { expanded = !expanded }
        ) {
            OutlinedTextField(
                value = productCategory,
                onValueChange = {},
                readOnly = true,
                label = { Text("Product Category") },
                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                modifier = Modifier
                    .fillMaxWidth()
                    .menuAnchor()
            )
            ExposedDropdownMenu(
                expanded = expanded,
                onDismissRequest = { expanded = false }
            ) {
                categories.forEach { category ->
                    DropdownMenuItem(
                        text = { Text(category) },
                        onClick = {
                            productCategory = category
                            expanded = false
                        }
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        Button(
            onClick = { addProduct(productName, productPrice, productCategory) },
            modifier = Modifier.fillMaxWidth(),
            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
        ) {
            Text("Add Product", fontSize = 18.sp)
        }

        Spacer(modifier = Modifier.height(8.dp))

        Button(
            onClick = {
                showProducts = !showProducts
                if (showProducts) fetchProducts()
            },
            modifier = Modifier.fillMaxWidth(),
            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary)
        ) {
            Text("View Products", fontSize = 18.sp)
        }

        Spacer(modifier = Modifier.height(8.dp))

        if (statusMessage.isNotEmpty()) {
            Text(
                text = statusMessage,
                color = if (statusMessage.contains("Error") || statusMessage.contains("fill"))
                    MaterialTheme.colorScheme.error
                else
                    MaterialTheme.colorScheme.primary,
                fontSize = 14.sp,
                modifier = Modifier.padding(8.dp)
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        if (showProducts) {
            Card(
                modifier = Modifier.fillMaxSize(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp)
                ) {
                    Text(
                        text = "Products List",
                        fontSize = 20.sp,
                        style = MaterialTheme.typography.titleLarge,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )

                    if (productList.isEmpty()) {
                        Text(
                            text = "No products to display",
                            modifier = Modifier.padding(16.dp)
                        )
                    } else {
                        LazyColumn {
                            items(productList.entries.toList()) { (name, product) ->
                                Card(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 4.dp),
                                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                                ) {
                                    Column(
                                        modifier = Modifier.padding(12.dp)
                                    ) {
                                        Text(
                                            text = "📦 $name",
                                            fontSize = 16.sp,
                                            style = MaterialTheme.typography.titleMedium
                                        )
                                        Text(
                                            text = "💰 Price: $${product.productPrice}",
                                            fontSize = 14.sp
                                        )
                                        Text(
                                            text = "🏷️ Category: ${product.productCategory}",
                                            fontSize = 14.sp,
                                            color = MaterialTheme.colorScheme.primary
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}