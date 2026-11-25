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
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.ime
import androidx.compose.ui.platform.LocalDensity
import com.photoedit.app.ui.components.PhotoEditNavigation
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.photoedit.app.ui.screens.ChatScreen
import com.photoedit.app.viewmodel.ChatViewModel
import com.photoedit.app.ui.screens.AiScreen
import com.photoedit.app.ui.screens.HomeScreen
import com.photoedit.app.ui.theme.PhotoEditTheme
import com.photoedit.app.viewmodel.AiViewModel
import com.photoedit.app.viewmodel.HomeViewModel
import com.photoedit.app.viewmodel.DressChangeViewModel
import com.photoedit.app.ui.screens.DressChangeScreen

class MainActivity : ComponentActivity() {
    private val chatViewModel: ChatViewModel by viewModels()
    private val homeViewModel: HomeViewModel by viewModels()
    private val aiViewModel: AiViewModel by viewModels()
    private val dressChangeViewModel: DressChangeViewModel by viewModels()
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            PhotoEditTheme {
                var tab by rememberSaveable { mutableIntStateOf(0) }
                val state by homeViewModel.uiState.collectAsStateWithLifecycle()
                val ai by aiViewModel.uiState.collectAsStateWithLifecycle()
                val chat by chatViewModel.uiState.collectAsStateWithLifecycle()
                val dressChange by dressChangeViewModel.uiState.collectAsStateWithLifecycle()
                val dressPicker = rememberLauncherForActivityResult(ActivityResultContracts.PickMultipleVisualMedia(30)) { dressChangeViewModel.selectPhotos(it) }
                val picker = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { homeViewModel.onPhotoSelected(it) }
                val referencePicker = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { aiViewModel.selectReference(it) }
                val gridPermission = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) {
                    if (it) homeViewModel.save() else homeViewModel.permissionDenied()
                }
                val videoPermission = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) {
                    if (it) dressChangeViewModel.saveVideo()
                }
                val aiPermission = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) {
                    if (it) aiViewModel.saveImage() else aiViewModel.permissionDenied()
                }
                val keyboardVisible = WindowInsets.ime.getBottom(LocalDensity.current) > 0
                Scaffold(bottomBar = {
                    if (!keyboardVisible) PhotoEditNavigation(tab, onSelect = { tab = it })
                }) { padding ->
                    androidx.compose.foundation.layout.Box(Modifier.fillMaxSize().padding(padding).consumeWindowInsets(padding)) {
                        if (tab == 0) HomeScreen(state, onChoosePhoto = {
                            picker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
                        }, onGrid = homeViewModel::selectGrid, onSplit = homeViewModel::split, onSave = {
                            if (needsStoragePermission()) gridPermission.launch(Manifest.permission.WRITE_EXTERNAL_STORAGE)
                            else homeViewModel.save()
                        }) else if (tab == 1) AiScreen(ai, aiViewModel::prompt, onChoose = {
                                referencePicker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
                            }, onRemove = aiViewModel::removeReference, onGenerate = aiViewModel::generate, onSaveImage = {
                                if (needsStoragePermission()) aiPermission.launch(Manifest.permission.WRITE_EXTERNAL_STORAGE)
                                else aiViewModel.saveImage()
                            }, onNewSession = aiViewModel::newSession, onUseTurn = aiViewModel::useTurn,
                            onContinue = aiViewModel::setContinueEditing,
                            onApiPodKey = aiViewModel::apiPodKey, onSaveApiPodKey = aiViewModel::saveApiPodKey,
                            onClearApiPodKey = aiViewModel::clearApiPodKey, onApiPodPro = aiViewModel::apiPodPro,
                            onStopApiPodWaiting = aiViewModel::stopApiPodWaiting,
                            onAbandonApiPodTracking = aiViewModel::abandonApiPodTracking) else if (tab == 3) DressChangeScreen(dressChange, onChoose = {
                            dressPicker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
                        }, onMove = dressChangeViewModel::move, onRemove = dressChangeViewModel::remove, onExport = dressChangeViewModel::export, onSave = {
                            if (needsStoragePermission()) videoPermission.launch(Manifest.permission.WRITE_EXTERNAL_STORAGE)
                            else dressChangeViewModel.saveVideo()
                        })
                        else ChatScreen(chat, chatViewModel::token, chatViewModel::saveKey, chatViewModel::clearKey,
                                chatViewModel::input, chatViewModel::mode, chatViewModel::send, chatViewModel::clearChat, chatViewModel::regenerateLast,
                                canUsePrompt = !ai.busy && !ai.apiPodPending, onUsePrompt = { prompt ->
                                    aiViewModel.prompt(prompt)
                                    tab = 1
                                }, onProvider = chatViewModel::provider, onModel = chatViewModel::model,
                                onModelDraft = chatViewModel::modelDraft,
                                onSavePromptRules = chatViewModel::savePromptRules, onResetPromptRules = chatViewModel::resetPromptRules)
                    }
                }
            }
        }
    }
    private fun needsStoragePermission() = Build.VERSION.SDK_INT <= 28 &&
        checkSelfPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED
}
