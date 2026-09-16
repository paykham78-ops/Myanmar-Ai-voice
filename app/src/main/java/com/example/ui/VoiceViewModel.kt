package com.example.ui

import android.app.Application
import android.content.Context
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.audio.AudioPlayerManager
import com.example.audio.AudioStorageHelper
import com.example.audio.PlayerState
import com.example.data.VoiceRepository
import com.example.data.local.AppDatabase
import com.example.data.local.VoiceEntity
import com.example.ui.theme.AppThemeMode
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class VoiceOption(
    val id: String,
    val name: String,
    val displayName: String,
    val gender: String,
    val descriptionBurmese: String,
    val toneEnglish: String
)

val AVAILABLE_VOICES = listOf(
    VoiceOption("Puck", "Puck", "Puck", "အမျိုးသား", "လန်းဆန်းသွက်လက်သောအသံ", "Energetic & clear"),
    VoiceOption("Charon", "Charon", "Charon", "အမျိုးသား", "တည်ငြိမ်လေးနက်သောအသံ", "Deep & authoritative"),
    VoiceOption("Kore", "Kore", "Kore", "အမျိုးသမီး", "နူးညံ့ငြိမ်းချမ်းသောအသံ", "Gentle & calming"),
    VoiceOption("Fenrir", "Fenrir", "Fenrir", "အမျိုးသား", "အားပါခိုင်မာသောအသံ", "Bold & powerful"),
    VoiceOption("Aoede", "Aoede", "Aoede", "အမျိုးသမီး", "သဘာဝကျနွေးထွေးသောအသံ", "Warm & natural"),
    VoiceOption("Leda", "Leda", "Leda", "အမျိုးသမီး", "သာယာကြည်လင်သောအသံ", "Bright & melodious"),
    VoiceOption("Orus", "Orus", "Orus", "အမျိုးသား", "တက်ကြွရဲရင့်သောအသံ", "Resonant & confident"),
    VoiceOption("Zephyr", "Zephyr", "Zephyr", "အမျိုးသမီး", "အေးချမ်းညင်သာသောအသံ", "Soft & serene")
)

val AVAILABLE_SPEEDS = listOf(0.75f, 1.0f, 1.25f)

data class VoiceUiState(
    val inputText: String = "မင်္ဂလာပါ ခင်ဗျာ။ မြန်မာ AI Voice မှ ကြိုဆိုပါတယ်။ မြန်မာစာကို သဘာဝကျကျ အသံဖန်တီးပေးနိုင်ပါသည်။",
    val selectedVoice: VoiceOption = AVAILABLE_VOICES[0], // Puck
    val selectedSpeed: Float = 1.0f,
    val isGenerating: Boolean = false,
    val errorMessage: String? = null,
    val successMessage: String? = null,
    val currentVoice: VoiceEntity? = null,
    val themeMode: AppThemeMode = AppThemeMode.SYSTEM
)

class VoiceViewModel(application: Application) : AndroidViewModel(application) {

    private val database = AppDatabase.getDatabase(application)
    private val repository = VoiceRepository(application, database.voiceDao())
    val playerManager = AudioPlayerManager(application, viewModelScope)

    val playerState: StateFlow<PlayerState> = playerManager.playerState

    val historyList: StateFlow<List<VoiceEntity>> = repository.allHistory
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    private val _uiState = MutableStateFlow(VoiceUiState())
    val uiState: StateFlow<VoiceUiState> = _uiState.asStateFlow()

    init {
        // Auto-load most recent voice into player if available
        viewModelScope.launch {
            historyList.collect { list ->
                if (_uiState.value.currentVoice == null && list.isNotEmpty()) {
                    val first = list.first()
                    _uiState.update { it.copy(currentVoice = first) }
                    playerManager.loadAndPlay(first.audioPath, first.speed, autoPlay = false)
                }
            }
        }
    }

    fun onInputTextChanged(newText: String) {
        if (newText.length <= 5000) {
            _uiState.update { it.copy(inputText = newText, errorMessage = null) }
        }
    }

    fun clearInputText() {
        _uiState.update { it.copy(inputText = "", errorMessage = null) }
    }

    fun onVoiceSelected(voice: VoiceOption) {
        _uiState.update { it.copy(selectedVoice = voice) }
    }

    fun onSpeedSelected(speed: Float) {
        _uiState.update { it.copy(selectedSpeed = speed) }
        playerManager.setSpeed(speed)
    }

    fun onVolumeChanged(volume: Float) {
        playerManager.setVolume(volume)
    }

    fun toggleThemeMode() {
        val nextMode = when (_uiState.value.themeMode) {
            AppThemeMode.SYSTEM -> AppThemeMode.LIGHT
            AppThemeMode.LIGHT -> AppThemeMode.DARK
            AppThemeMode.DARK -> AppThemeMode.SYSTEM
        }
        _uiState.update { it.copy(themeMode = nextMode) }
    }

    fun dismissError() {
        _uiState.update { it.copy(errorMessage = null) }
    }

    fun generateVoice() {
        // Prevent duplicate generation while a request is in progress
        if (_uiState.value.isGenerating) {
            return
        }

        val text = _uiState.value.inputText.trim()
        if (text.isBlank()) {
            _uiState.update { it.copy(errorMessage = "ကျေးဇူးပြု၍ မြန်မာစာသား အရင်ရိုက်ထည့်ပါ") }
            return
        }

        if (text.length > 5000) {
            _uiState.update { it.copy(errorMessage = "စာလုံးရေ ၅,၀၀၀ ထက် မပိုရပါ") }
            return
        }

        val voice = _uiState.value.selectedVoice
        val speed = _uiState.value.selectedSpeed

        _uiState.update { it.copy(isGenerating = true, errorMessage = null, successMessage = null) }

        viewModelScope.launch {
            val result = repository.generateVoice(
                text = text,
                voiceName = voice.name,
                speed = speed
            )

            result.fold(
                onSuccess = { entity ->
                    _uiState.update {
                        it.copy(
                            isGenerating = false,
                            currentVoice = entity,
                            successMessage = "အသံဖန်တီးမှု အောင်မြင်ပါသည်"
                        )
                    }
                    playerManager.loadAndPlay(entity.audioPath, entity.speed, autoPlay = true)
                },
                onFailure = { error ->
                    _uiState.update {
                        it.copy(
                            isGenerating = false,
                            errorMessage = error.localizedMessage ?: "အသံဖန်တီးရာတွင် အမှားဖြစ်ပေါ်ပါသည်"
                        )
                    }
                }
            )
        }
    }

    fun playVoice(entity: VoiceEntity) {
        _uiState.update { it.copy(currentVoice = entity) }
        playerManager.loadAndPlay(entity.audioPath, entity.speed, autoPlay = true)
    }

    fun togglePlayPause() {
        if (!playerState.value.isLoaded) {
            val current = _uiState.value.currentVoice
            if (current != null) {
                playerManager.loadAndPlay(current.audioPath, current.speed, autoPlay = true)
                return
            }
        }
        playerManager.togglePlayPause()
    }

    fun seekTo(positionMs: Int) {
        playerManager.seekTo(positionMs)
    }

    fun downloadCurrentVoice(context: Context) {
        val current = _uiState.value.currentVoice
        if (current != null) {
            AudioStorageHelper.downloadAudio(context, current.audioPath)
        }
    }

    fun shareCurrentVoice(context: Context) {
        val current = _uiState.value.currentVoice
        if (current != null) {
            AudioStorageHelper.shareAudio(context, current.audioPath, current.text)
        }
    }

    fun downloadVoice(context: Context, entity: VoiceEntity) {
        AudioStorageHelper.downloadAudio(context, entity.audioPath)
    }

    fun shareVoice(context: Context, entity: VoiceEntity) {
        AudioStorageHelper.shareAudio(context, entity.audioPath, entity.text)
    }

    fun deleteVoice(entity: VoiceEntity) {
        viewModelScope.launch {
            if (_uiState.value.currentVoice?.id == entity.id) {
                playerManager.stop()
                _uiState.update { it.copy(currentVoice = null) }
            }
            repository.delete(entity)
        }
    }

    fun clearHistory() {
        viewModelScope.launch {
            playerManager.stop()
            _uiState.update { it.copy(currentVoice = null) }
            repository.clearAll()
        }
    }

    override fun onCleared() {
        super.onCleared()
        playerManager.release()
    }
}
