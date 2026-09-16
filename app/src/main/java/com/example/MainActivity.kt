package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.ui.VoiceScreen
import com.example.ui.VoiceViewModel
import com.example.ui.theme.MyanmarVoiceTheme

class MainActivity : ComponentActivity() {
  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    enableEdgeToEdge()
    setContent {
      val voiceViewModel: VoiceViewModel = viewModel()
      val uiState by voiceViewModel.uiState.collectAsStateWithLifecycle()

      MyanmarVoiceTheme(themeMode = uiState.themeMode) {
        Surface(modifier = Modifier.fillMaxSize()) {
          VoiceScreen(viewModel = voiceViewModel)
        }
      }
    }
  }
}


