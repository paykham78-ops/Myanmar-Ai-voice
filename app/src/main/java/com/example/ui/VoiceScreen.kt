package com.example.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Brightness4
import androidx.compose.material.icons.filled.Brightness7
import androidx.compose.material.icons.filled.BrightnessAuto
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.R
import com.example.ui.components.AudioPlayer
import com.example.ui.components.BurmeseTextInput
import com.example.ui.components.ErrorAlert
import com.example.ui.components.GenerateButton
import com.example.ui.components.HistoryList
import com.example.ui.components.LoadingState
import com.example.ui.components.VoiceSelector
import com.example.ui.theme.AppThemeMode

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VoiceScreen(viewModel: VoiceViewModel) {
    val context = LocalContext.current
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val playerState by viewModel.playerState.collectAsStateWithLifecycle()
    val historyList by viewModel.historyList.collectAsStateWithLifecycle()

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = MaterialTheme.colorScheme.primaryContainer,
                            modifier = Modifier.size(36.dp)
                        ) {
                            Image(
                                painter = painterResource(id = R.drawable.myanmar_ai_voice_logo),
                                contentDescription = "Logo",
                                contentScale = ContentScale.Crop,
                                modifier = Modifier.fillMaxSize()
                            )
                        }
                        Column {
                            Text(
                                text = stringResource(id = R.string.app_name),
                                style = MaterialTheme.typography.titleMedium.copy(
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 17.sp
                                )
                            )
                            Text(
                                text = "Burmese AI Voice Generator",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    color = MaterialTheme.colorScheme.primary,
                                    fontSize = 10.sp
                                )
                            )
                        }
                    }
                },
                actions = {
                    // Dark / Light / System Mode Toggle
                    IconButton(
                        onClick = viewModel::toggleThemeMode,
                        modifier = Modifier.testTag("theme_toggle_button")
                    ) {
                        val (icon, desc) = when (uiState.themeMode) {
                            AppThemeMode.SYSTEM -> Icons.Default.BrightnessAuto to "System theme"
                            AppThemeMode.LIGHT -> Icons.Default.Brightness7 to "Light theme"
                            AppThemeMode.DARK -> Icons.Default.Brightness4 to "Dark theme"
                        }
                        Icon(
                            imageVector = icon,
                            contentDescription = desc,
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        },
        contentWindowInsets = WindowInsets(0, 0, 0, 0)
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                Spacer(modifier = Modifier.height(2.dp))
                HeroHeaderCard()
            }

            // Error Alert Banner
            if (uiState.errorMessage != null) {
                item {
                    ErrorAlert(
                        message = uiState.errorMessage,
                        onDismiss = viewModel::dismissError
                    )
                }
            }

            // 1. Burmese text input textarea with character counter & clear button
            item {
                BurmeseTextInput(
                    text = uiState.inputText,
                    onTextChanged = viewModel::onInputTextChanged,
                    onClear = viewModel::clearInputText
                )
            }

            // 2. Voice selector (Puck, Charon, Kore, Fenrir, Aoede, Leda, Orus, Zephyr)
            item {
                VoiceSelector(
                    selectedVoice = uiState.selectedVoice,
                    availableVoices = AVAILABLE_VOICES,
                    onVoiceSelected = viewModel::onVoiceSelected
                )
            }

            // 3. Speech Speed Controls
            item {
                SpeedControls(
                    selectedSpeed = uiState.selectedSpeed,
                    onSpeedSelected = viewModel::onSpeedSelected
                )
            }

            // 4. Generate Burmese Voice button (Prominent, prevents duplicate request)
            item {
                GenerateButton(
                    isGenerating = uiState.isGenerating,
                    onClick = viewModel::generateVoice
                )
            }

            // 5. Loading Skeleton & Progress State
            item {
                AnimatedVisibility(
                    visible = uiState.isGenerating,
                    enter = fadeIn(),
                    exit = fadeOut()
                ) {
                    LoadingState()
                }
            }

            item {
                HorizontalDivider(
                    modifier = Modifier.padding(vertical = 4.dp),
                    color = MaterialTheme.colorScheme.outline.copy(alpha = 0.4f)
                )
            }

            // 6. Audio Player (Play, Pause, Seek, Volume, Download, Share)
            item {
                AudioPlayer(
                    currentVoice = uiState.currentVoice,
                    isPlaying = playerState.isPlaying,
                    isLoaded = playerState.isLoaded,
                    currentPositionMs = playerState.currentPositionMs,
                    durationMs = playerState.durationMs,
                    volume = playerState.volume,
                    onPlayPauseToggle = viewModel::togglePlayPause,
                    onSeek = viewModel::seekTo,
                    onVolumeChange = viewModel::onVolumeChanged,
                    onDownload = { viewModel.downloadCurrentVoice(context) },
                    onShare = { viewModel.shareCurrentVoice(context) }
                )
            }

            item {
                HorizontalDivider(
                    modifier = Modifier.padding(vertical = 4.dp),
                    color = MaterialTheme.colorScheme.outline.copy(alpha = 0.4f)
                )
            }

            // 7. Generation History List
            item {
                HistoryList(
                    historyList = historyList,
                    currentPlayingId = uiState.currentVoice?.id,
                    isPlaying = playerState.isPlaying,
                    onPlayPause = { voiceItem ->
                        if (uiState.currentVoice?.id == voiceItem.id) {
                            viewModel.togglePlayPause()
                        } else {
                            viewModel.playVoice(voiceItem)
                        }
                    },
                    onDownload = { viewModel.downloadVoice(context, it) },
                    onShare = { viewModel.shareVoice(context, it) },
                    onDelete = { viewModel.deleteVoice(it) },
                    onClearAll = viewModel::clearHistory
                )
            }

            item {
                Spacer(modifier = Modifier.height(32.dp))
            }
        }
    }
}

@Composable
private fun HeroHeaderCard() {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f)
        ),
        border = CardDefaults.outlinedCardBorder()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(52.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(
                        Brush.linearGradient(
                            listOf(
                                MaterialTheme.colorScheme.primary,
                                MaterialTheme.colorScheme.secondary
                            )
                        )
                    )
                    .padding(2.dp)
            ) {
                Image(
                    painter = painterResource(id = R.drawable.myanmar_ai_voice_logo),
                    contentDescription = "App Logo",
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .fillMaxSize()
                        .clip(RoundedCornerShape(12.dp))
                )
            }

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Myanmar AI Voice",
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.ExtraBold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = stringResource(id = R.string.app_tagline),
                    style = MaterialTheme.typography.bodySmall.copy(
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.SemiBold
                    )
                )
            }
        }
    }
}

@Composable
private fun SpeedControls(
    selectedSpeed: Float,
    onSpeedSelected: (Float) -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Icon(
                imageVector = Icons.Default.Speed,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(18.dp)
            )
            Text(
                text = stringResource(id = R.string.voice_speed),
                style = MaterialTheme.typography.titleMedium.copy(
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
            )
        }

        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            AVAILABLE_SPEEDS.forEach { speed ->
                val isSelected = selectedSpeed == speed
                FilterChip(
                    selected = isSelected,
                    onClick = { onSpeedSelected(speed) },
                    label = {
                        Text(
                            text = "${speed}x",
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                        )
                    },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = MaterialTheme.colorScheme.primary,
                        selectedLabelColor = MaterialTheme.colorScheme.onPrimary
                    ),
                    modifier = Modifier.testTag("speed_chip_${speed}x")
                )
            }
        }
    }
}
