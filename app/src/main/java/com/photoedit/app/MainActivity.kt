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
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.photoedit.app.ui.screens.ChatScreen
import com.photoedit.app.viewmodel.ChatViewModel
import com.photoedit.app.ui.screens.AiScreen
import com.photoedit.app.ui.screens.HomeScreen
import com.photoedit.app.ui.theme.PhotoEditTheme
import com.photoedit.app.viewmodel.AiViewModel
import com.photoedit.app.viewmodel.HomeViewModel

class MainActivity : ComponentActivity() {
    private val chatViewModel: ChatViewModel by viewModels()
    private val homeViewModel: HomeViewModel by viewModels()
    private val aiViewModel: AiViewModel by viewModels()
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            PhotoEditTheme {
                var tab by rememberSaveable { mutableIntStateOf(0) }
                val state by homeViewModel.uiState.collectAsStateWithLifecycle()
                val ai by aiViewModel.uiState.collectAsStateWithLifecycle()
                val chat by chatViewModel.uiState.collectAsStateWithLifecycle()
                val picker = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { homeViewModel.onPhotoSelected(it) }
                val referencePicker = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { aiViewModel.selectReference(it) }
                val gridPermission = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) {
                    if (it) homeViewModel.save() else homeViewModel.permissionDenied()
                }
                val aiPermission = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) {
                    if (it) aiViewModel.saveImage() else aiViewModel.permissionDenied()
                }
                Scaffold { padding ->
                    Column(Modifier.fillMaxSize().padding(padding)) {
                        TabRow(selectedTabIndex = tab) {
                            Tab(selected = tab == 0, onClick = { tab = 0 }, text = { Text("离线宫格") })
                            Tab(selected = tab == 1, onClick = { tab = 1 }, text = { Text("AI 图片") })
                            Tab(selected = tab == 2, onClick = { tab = 2 }, text = { Text("提示词助手") })
                        }
                        androidx.compose.foundation.layout.Box(Modifier.weight(1f)) {
                            if (tab == 0) HomeScreen(state, onChoosePhoto = {
                                picker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
                            }, onGrid = homeViewModel::selectGrid, onSplit = homeViewModel::split, onSave = {
                                if (needsStoragePermission()) gridPermission.launch(Manifest.permission.WRITE_EXTERNAL_STORAGE)
                                else homeViewModel.save()
                            }) else if (tab == 1) AiScreen(ai, aiViewModel::account, aiViewModel::token, aiViewModel::prompt,
                                aiViewModel::saveSettings, aiViewModel::clearSettings, onChoose = {
                                    referencePicker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
                                }, onRemove = aiViewModel::removeReference, onGenerate = aiViewModel::generate, onSaveImage = {
                                    if (needsStoragePermission()) aiPermission.launch(Manifest.permission.WRITE_EXTERNAL_STORAGE)
                                    else aiViewModel.saveImage()
                                }) else ChatScreen(chat, chatViewModel::token, chatViewModel::saveKey, chatViewModel::clearKey,
                                    chatViewModel::input, chatViewModel::mode, chatViewModel::send, chatViewModel::clearChat,
                                    canUsePrompt = !ai.busy, onUsePrompt = { prompt ->
                                        aiViewModel.prompt(prompt)
                                        tab = 1
                                    }, onProvider = chatViewModel::provider, onModel = chatViewModel::model,
                                    onModelDraft = chatViewModel::modelDraft, onRefreshModels = chatViewModel::refreshModels,
                                    onAllowPaid = chatViewModel::allowPaid)
                        }
                    }
                }
            }
        }
    }
    private fun needsStoragePermission() = Build.VERSION.SDK_INT <= 28 &&
        checkSelfPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED
}
