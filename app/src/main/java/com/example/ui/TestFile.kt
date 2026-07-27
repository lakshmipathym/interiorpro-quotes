package com.example.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.CardDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color

@Composable
fun TestComponent() {
    ElevatedCard(
        modifier = Modifier,
        colors = CardDefaults.elevatedCardColors(containerColor = Color.Red)) {
        Column {
        }
    }
}
