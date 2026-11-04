package com.photoedit.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.photoedit.app.ui.screens.HomeScreen
import com.photoedit.app.viewmodel.HomeViewModel

class MainActivity : ComponentActivity() {
    private val homeViewModel: HomeViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme {
                val state by homeViewModel.uiState.collectAsStateWithLifecycle()
                val picker = rememberLauncherForActivityResult(
                    ActivityResultContracts.GetContent()
                ) { uri -> homeViewModel.onPhotoSelected(uri) }
                HomeScreen(state = state, onChoosePhoto = { picker.launch("image/*") })
            }
        }
    }
}
