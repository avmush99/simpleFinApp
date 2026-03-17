package com.simpleFinApp.models

import java.util.UUID

data class Spending(
    val id: String = UUID.randomUUID().toString(),
    val amount: Double,
    val label: String,
    val note: String = "",
    val timestamp: Long = System.currentTimeMillis()
)
