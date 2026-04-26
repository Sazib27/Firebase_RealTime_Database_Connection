package com.example.firebaseapp

import androidx.annotation.Keep

@Keep
data class Product(
    val productPrice: String = "",
    val productCategory: String = ""
)