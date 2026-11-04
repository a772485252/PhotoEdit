package com.photoedit.app

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.photoedit.app.ui.screens.HomeScreen
import com.photoedit.app.ui.theme.PhotoEditTheme
import com.photoedit.app.viewmodel.HomeViewModel

class MainActivity : ComponentActivity() {
    private val homeViewModel: HomeViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            PhotoEditTheme {
                val state by homeViewModel.uiState.collectAsStateWithLifecycle()
                val picker = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) {
                    uri -> homeViewModel.onPhotoSelected(uri)
                }
                val permission = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
                    if (granted) homeViewModel.save() else homeViewModel.permissionDenied()
                }
                HomeScreen(state, onChoosePhoto = {
                    picker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
                }, onGrid = homeViewModel::selectGrid, onSplit = homeViewModel::split, onSave = {
                    if (Build.VERSION.SDK_INT <= 28 && checkSelfPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
                        permission.launch(Manifest.permission.WRITE_EXTERNAL_STORAGE)
                    } else homeViewModel.save()
                })
            }
        }
    }
}
