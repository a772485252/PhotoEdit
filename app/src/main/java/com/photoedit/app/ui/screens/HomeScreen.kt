package com.photoedit.app.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.photoedit.app.viewmodel.HomeUiState

@Composable
fun HomeScreen(state: HomeUiState, onChoosePhoto: () -> Unit) {
    Scaffold { padding ->
        Column(
            modifier = Modifier.fillMaxSize().padding(padding).padding(24.dp),
            verticalArrangement = Arrangement.Center
        ) {
            Text("PhotoEdit", style = MaterialTheme.typography.headlineLarge)
            Spacer(Modifier.height(12.dp))
            Text("Native Android photo tools · Offline first")
            Spacer(Modifier.height(24.dp))
            Button(onClick = onChoosePhoto, modifier = Modifier.fillMaxWidth()) {
                Text("Choose a photo")
            }
            Spacer(Modifier.height(12.dp))
            Text(if (state.hasSelectedImage) "Photo selected. Grid splitting is coming next."
                else "No photo selected")
            Spacer(Modifier.height(24.dp))
            Text("Next: 2×2 / 3×3 / 4×4 grid splitter")
        }
    }
}
