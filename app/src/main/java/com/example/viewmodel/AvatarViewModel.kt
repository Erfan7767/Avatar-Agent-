package com.example.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.audio.AndroidSpeechRecognizerProvider
import com.example.audio.AndroidTextToSpeechProvider
import com.example.audio.AsrListener
import com.example.audio.AudioRecordManager
import com.example.audio.IAsrProvider
import com.example.audio.ITtsProvider
import com.example.audio.TtsListener
import com.example.avatar.AvatarAnimationRig
import com.example.data.local.AvatarDatabase
import com.example.data.local.AvatarRepository
import com.example.data.local.PersistentMemoryEntity
import com.example.engine.ConversationEngine
import com.example.engine.ILlmProvider
import com.example.model.AppSettings
import com.example.model.AuditLogEntry
import com.example.model.AvailableCharacters
import com.example.model.AvatarCharacter
import com.example.model.AvatarState
import com.example.model.ConversationTurn
import com.example.model.EvidenceClassification
import com.example.model.FacialExpression
import com.example.model.LanguageMode
import com.example.model.OperationStatus
import com.example.model.PersonaStyle
import com.example.model.ToolCallInfo
import com.example.model.VisemeFrame
import com.example.model.VoiceProfile
import com.example.sentiment.ContextAwareSentimentAnalyzer
import com.example.sentiment.SentimentAnalysisResult
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.util.UUID

class AvatarViewModel @JvmOverloads constructor(
    application: Application,
    private val repository: AvatarRepository = AvatarRepository(AvatarDatabase.getDatabase(application)),
    private val asrProvider: IAsrProvider = AndroidSpeechRecognizerProvider(application),
    private val ttsProvider: ITtsProvider = AndroidTextToSpeechProvider(application),
    private val llmProvider: ILlmProvider = ConversationEngine(application),
    private val audioRecorder: AudioRecordManager = AudioRecordManager(application)
) : AndroidViewModel(application) {

    private var sessionId = UUID.randomUUID().toString()

    private val _avatarState = MutableStateFlow(AvatarState.IDLE)
    val avatarState: StateFlow<AvatarState> = _avatarState.asStateFlow()

    private val _activeToolCall = MutableStateFlow<ToolCallInfo?>(null)
    val activeToolCall: StateFlow<ToolCallInfo?> = _activeToolCall.asStateFlow()

    private val _visemeFrame = MutableStateFlow(VisemeFrame())
    val visemeFrame: StateFlow<VisemeFrame> = _visemeFrame.asStateFlow()

    private val _settings = MutableStateFlow(AppSettings())
    val settings: StateFlow<AppSettings> = _settings.asStateFlow()

    private val _selectedCharacter = MutableStateFlow(AvailableCharacters.CHARACTERS.first())
    val selectedCharacter: StateFlow<AvatarCharacter> = _selectedCharacter.asStateFlow()

    private val _selectedVoice = MutableStateFlow(AvailableCharacters.VOICES.first())
    val selectedVoice: StateFlow<VoiceProfile> = _selectedVoice.asStateFlow()

    private val _selectedPersona = MutableStateFlow(PersonaStyle.WORLD_CLASS_EXPERT)
    val selectedPersona: StateFlow<PersonaStyle> = _selectedPersona.asStateFlow()

    private val _speechAudioAmplitude = MutableStateFlow(0f)
    val speechAudioAmplitude: StateFlow<Float> = _speechAudioAmplitude.asStateFlow()

    private val _userAudioAmplitude = MutableStateFlow(0f)
    val userAudioAmplitude: StateFlow<Float> = _userAudioAmplitude.asStateFlow()

    private val _currentTranscript = MutableStateFlow("")
    val currentTranscript: StateFlow<String> = _currentTranscript.asStateFlow()

    private val _lastAvatarResponse = MutableStateFlow("")
    val lastAvatarResponse: StateFlow<String> = _lastAvatarResponse.asStateFlow()

    private val _isSessionActive = MutableStateFlow(false)
    val isSessionActive: StateFlow<Boolean> = _isSessionActive.asStateFlow()

    private val _isLiveCallActive = MutableStateFlow(false)
    val isLiveCallActive: StateFlow<Boolean> = _isLiveCallActive.asStateFlow()

    private val _isMicListening = MutableStateFlow(false)
    val isMicListening: StateFlow<Boolean> = _isMicListening.asStateFlow()

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

    private val _conversationTurns = MutableStateFlow<List<ConversationTurn>>(emptyList())
    val conversationTurns: StateFlow<List<ConversationTurn>> = _conversationTurns.asStateFlow()

    private val _persistentMemories = MutableStateFlow<List<PersistentMemoryEntity>>(emptyList())
    val persistentMemories: StateFlow<List<PersistentMemoryEntity>> = _persistentMemories.asStateFlow()

    private val _auditLogs = MutableStateFlow<List<AuditLogEntry>>(emptyList())
    val auditLogs: StateFlow<List<AuditLogEntry>> = _auditLogs.asStateFlow()

    private val sentimentAnalyzer = ContextAwareSentimentAnalyzer()
    private val _currentSentiment = MutableStateFlow<SentimentAnalysisResult?>(null)
    val currentSentiment: StateFlow<SentimentAnalysisResult?> = _currentSentiment.asStateFlow()

    private val animationRig = AvatarAnimationRig()
    private var animationLoopJob: Job? = null
    private var speechEnvelopeJob: Job? = null
    private var fallbackSpeechJob: Job? = null
    private var liveCallLoopJob: Job? = null
    private var currentExpression = FacialExpression.NEUTRAL

    init {
        // Initialize TTS
        ttsProvider.initialize { success ->
            viewModelScope.launch {
                repository.logAudit(
                    sessionId = sessionId,
                    eventType = "TTS Engine Initialization",
                    details = if (success) "TTS initialized successfully" else "TTS initialization returned false",
                    status = if (success) OperationStatus.SUCCEEDED else OperationStatus.FAILED
                )
            }
        }

        // Collect persistent memories
        viewModelScope.launch {
            repository.allMemories.collect { list ->
                _persistentMemories.value = list
            }
        }

        // Collect audit logs
        viewModelScope.launch {
            repository.allLogs.collect { logs ->
                _auditLogs.value = logs
            }
        }

        startAnimationLoop()
    }

    private fun startAnimationLoop() {
        animationLoopJob?.cancel()
        animationLoopJob = viewModelScope.launch {
            while (isActive) {
                val now = System.currentTimeMillis()
                val frame = animationRig.updateFrame(
                    currentTimeMs = now,
                    state = _avatarState.value,
                    speechAudioAmplitude = _speechAudioAmplitude.value,
                    contextExpression = currentExpression
                )
                _visemeFrame.value = frame
                delay(33) // ~30 FPS smooth animation
            }
        }
    }

    fun startSession() {
        sessionId = UUID.randomUUID().toString()
        _isSessionActive.value = true
        _avatarState.value = AvatarState.IDLE
        _errorMessage.value = null
        currentExpression = FacialExpression.WARM_SMILE

        viewModelScope.launch {
            repository.logAudit(
                sessionId = sessionId,
                eventType = "Session Started",
                details = "Session initialized with language=${_settings.value.languageMode.displayName}, character=${_selectedCharacter.value.nameEn}, voice=${_selectedVoice.value.nameEn}",
                status = OperationStatus.SUCCEEDED
            )
        }

        // Speak opening greeting
        val greeting = if (_settings.value.languageMode == LanguageMode.ARABIC) {
            _selectedCharacter.value.greetingAr
        } else {
            _selectedCharacter.value.greetingEn
        }
        speakAvatarResponse(greeting, FacialExpression.WARM_SMILE, EvidenceClassification.VERIFIED)
    }

    fun endSession() {
        ttsProvider.stop()
        asrProvider.stopListening()
        audioRecorder.stopMonitoring()
        _isSessionActive.value = false
        _isMicListening.value = false
        _avatarState.value = AvatarState.DISCONNECTED
        _speechAudioAmplitude.value = 0f
        _userAudioAmplitude.value = 0f
        animationRig.reset()

        viewModelScope.launch {
            repository.logAudit(
                sessionId = sessionId,
                eventType = "Session Ended",
                details = "User ended session",
                status = OperationStatus.SUCCEEDED
            )
        }
    }

    fun toggleListening() {
        if (_isMicListening.value) {
            stopListening()
        } else {
            startListening()
        }
    }

    /**
     * Start continuous hands-free Live Voice Call with the AI Avatar.
     * Functions like talking on a phone call or face-to-face with a real person.
     */
    fun startLiveCall() {
        _isLiveCallActive.value = true
        _isSessionActive.value = true
        liveCallLoopJob?.cancel()
        viewModelScope.launch {
            repository.logAudit(
                sessionId = sessionId,
                eventType = "Live Voice Call Started",
                details = "Real-time hands-free voice call activated",
                status = OperationStatus.SUCCEEDED
            )
        }
        if (_avatarState.value != AvatarState.SPEAKING) {
            startListening()
        }
    }

    /**
     * Terminate the continuous Live Voice Call mode.
     */
    fun stopLiveCall() {
        _isLiveCallActive.value = false
        liveCallLoopJob?.cancel()
        liveCallLoopJob = null
        stopListening()
        if (_avatarState.value == AvatarState.SPEAKING) {
            ttsProvider.stop()
            fallbackSpeechJob?.cancel()
            stopLipSyncSpeechEnvelope()
            _avatarState.value = AvatarState.IDLE
        }
        viewModelScope.launch {
            repository.logAudit(
                sessionId = sessionId,
                eventType = "Live Voice Call Ended",
                details = "User ended real-time voice call session",
                status = OperationStatus.SUCCEEDED
            )
        }
    }

    fun toggleLiveCall() {
        if (_isLiveCallActive.value) {
            stopLiveCall()
        } else {
            startLiveCall()
        }
    }

    fun startListening() {
        // If avatar is currently speaking, barge-in!
        if (_avatarState.value == AvatarState.SPEAKING) {
            handleBargeInInterruption()
            return
        }

        liveCallLoopJob?.cancel()
        _isMicListening.value = true
        _avatarState.value = AvatarState.LISTENING
        currentExpression = FacialExpression.LISTENING_NOD
        _currentTranscript.value = ""

        viewModelScope.launch {
            repository.logAudit(
                sessionId = sessionId,
                eventType = "Microphone Activated",
                details = "Listening for user speech in language ${_settings.value.languageMode.name}",
                status = OperationStatus.STARTED
            )
        }

        // Start VAD audio amplitude monitoring with real-time barge-in trigger
        audioRecorder.startMonitoring(
            scope = viewModelScope,
            onAmplitude = { amp ->
                _userAudioAmplitude.value = amp
                // If avatar is speaking and live call is on, interrupt if user speaks loud
                if (_avatarState.value == AvatarState.SPEAKING && amp > 0.45f && _isLiveCallActive.value) {
                    handleBargeInInterruption()
                }
            },
            onVoiceDetected = { detected ->
                if (detected && _avatarState.value == AvatarState.SPEAKING && _isLiveCallActive.value) {
                    handleBargeInInterruption()
                }
            }
        )

        // Start speech recognizer
        asrProvider.startListening(
            language = _settings.value.languageMode,
            listener = object : AsrListener {
                override fun onReadyForSpeech() {
                    _avatarState.value = AvatarState.LISTENING
                }

                override fun onRmsChanged(rmsdB: Float) {
                    val norm = ((rmsdB + 2f) / 12f).coerceIn(0f, 1f)
                    _userAudioAmplitude.value = norm
                }

                override fun onPartialResults(partialText: String) {
                    _currentTranscript.value = partialText
                }

                override fun onFinalResult(text: String) {
                    _isMicListening.value = false
                    audioRecorder.stopMonitoring()
                    _userAudioAmplitude.value = 0f
                    if (text.isNotBlank()) {
                        _currentTranscript.value = text
                        processUserSpeech(text)
                    } else {
                        _avatarState.value = AvatarState.IDLE
                        if (_isLiveCallActive.value) {
                            // Seamless loop in Live Call: resume listening after short breather
                            liveCallLoopJob?.cancel()
                            liveCallLoopJob = viewModelScope.launch {
                                delay(350)
                                if (_isLiveCallActive.value && _avatarState.value == AvatarState.IDLE) {
                                    startListening()
                                }
                            }
                        }
                    }
                }

                override fun onEndOfSpeech() {
                    _isMicListening.value = false
                    audioRecorder.stopMonitoring()
                    _avatarState.value = AvatarState.THINKING
                }

                override fun onError(errorCode: Int, errorMessage: String) {
                    _isMicListening.value = false
                    audioRecorder.stopMonitoring()
                    _userAudioAmplitude.value = 0f

                    // If speech recognizer timed out or couldn't match final text, but we got partial text, use it!
                    val partial = _currentTranscript.value.trim()
                    if (partial.isNotBlank() && (errorCode == 7 || errorCode == 6)) { // ERROR_NO_MATCH=7, ERROR_SPEECH_TIMEOUT=6
                        _avatarState.value = AvatarState.THINKING
                        processUserSpeech(partial)
                        return
                    }

                    _avatarState.value = AvatarState.IDLE

                    if (_isLiveCallActive.value) {
                        // Live Call Mode: If user was silent or recognizer timed out, gracefully re-listen
                        liveCallLoopJob?.cancel()
                        liveCallLoopJob = viewModelScope.launch {
                            delay(400)
                            if (_isLiveCallActive.value && _avatarState.value == AvatarState.IDLE) {
                                startListening()
                            }
                        }
                    } else {
                        // For harmless timeouts where user just paused, do not show an alarming error banner
                        if (errorCode != 7 && errorCode != 6) {
                            _errorMessage.value = errorMessage
                        }
                    }

                    viewModelScope.launch {
                        repository.logAudit(
                            sessionId = sessionId,
                            eventType = "ASR Status",
                            details = "Code $errorCode: $errorMessage",
                            status = OperationStatus.FAILED
                        )
                    }
                }
            }
        )
    }

    fun stopListening() {
        _isMicListening.value = false
        audioRecorder.stopMonitoring()
        asrProvider.stopListening()
        _userAudioAmplitude.value = 0f
        if (_avatarState.value == AvatarState.LISTENING) {
            _avatarState.value = AvatarState.IDLE
        }
    }

    /**
     * Barge-In Interruption:
     * When avatar is speaking and user speaks or presses mic,
     * the avatar immediately halts speech, resets mouth, and transitions to LISTENING.
     */
    fun handleBargeInInterruption() {
        if (_avatarState.value == AvatarState.SPEAKING) {
            ttsProvider.stop()
            fallbackSpeechJob?.cancel()
            liveCallLoopJob?.cancel()
            speechEnvelopeJob?.cancel()
            _speechAudioAmplitude.value = 0f
            _avatarState.value = AvatarState.INTERRUPTED
            animationRig.reset()

            viewModelScope.launch {
                repository.logAudit(
                    sessionId = sessionId,
                    eventType = "Avatar Interrupted",
                    details = "Barge-In triggered: Avatar audio and lip sync stopped",
                    status = OperationStatus.SUCCEEDED
                )
                delay(150)
                startListening()
            }
        }
    }

    fun processUserSpeech(userSpeech: String) {
        val asrTimestamp = System.currentTimeMillis()
        val userTurn = ConversationTurn(
            role = "user",
            content = userSpeech,
            language = _settings.value.languageMode.code,
            evidenceLevel = EvidenceClassification.VERIFIED,
            timestamp = asrTimestamp
        )
        _conversationTurns.value = _conversationTurns.value + userTurn

        // Real-time Context-Aware Sentiment Analysis
        val sentiment = sentimentAnalyzer.analyze(userSpeech, _conversationTurns.value)
        _currentSentiment.value = sentiment

        viewModelScope.launch {
            repository.saveConversationTurn(sessionId, userTurn)
            repository.logAudit(
                sessionId = sessionId,
                eventType = "User Speech Received",
                details = "Transcript: \"$userSpeech\" | Emotion: ${sentiment.category.emoji} ${sentiment.category.labelEn}",
                status = OperationStatus.SUCCEEDED
            )

            _avatarState.value = AvatarState.THINKING
            currentExpression = sentiment.recommendedExpression

            val memoryStrings = _persistentMemories.value.map { "${it.keyName}: ${it.valueContent}" }

            val responseResult = llmProvider.generateResponse(
                prompt = userSpeech,
                character = _selectedCharacter.value,
                persona = _selectedPersona.value,
                language = _settings.value.languageMode,
                history = _conversationTurns.value,
                persistentMemories = memoryStrings,
                isCameraActive = _settings.value.userCameraEnabled,
                onToolCallExecuted = { toolInfo ->
                    _activeToolCall.value = toolInfo
                    viewModelScope.launch {
                        repository.logAudit(
                            sessionId = sessionId,
                            eventType = "Tool Executed",
                            details = "${toolInfo.toolIcon} ${toolInfo.toolName}: \"${toolInfo.queryOrArg}\" -> ${toolInfo.resultPreview}",
                            status = OperationStatus.SUCCEEDED
                        )
                    }
                }
            )

            val toolBadge = if (responseResult.toolCalls.isNotEmpty()) {
                val firstTool = responseResult.toolCalls.first()
                "${firstTool.toolIcon} ${firstTool.toolName}(\"${firstTool.queryOrArg}\")"
            } else null

            // Prioritize sentiment analysis recommended expression, falling back to LLM response expression
            val finalExpression = if (sentiment.category != com.example.sentiment.SentimentCategory.NEUTRAL_CALM) {
                sentiment.recommendedExpression
            } else {
                responseResult.expression
            }

            speakAvatarResponse(
                text = responseResult.responseText,
                expression = finalExpression,
                evidenceClassification = responseResult.evidenceClassification,
                llmLatency = responseResult.latencyMs,
                toolBadge = toolBadge,
                vocalPitchAdjustment = sentiment.recommendedPitchMultiplier,
                vocalRateAdjustment = sentiment.recommendedRateMultiplier
            )
        }
    }

    private fun speakAvatarResponse(
        text: String,
        expression: FacialExpression,
        evidenceClassification: EvidenceClassification,
        llmLatency: Long = 0,
        toolBadge: String? = null,
        vocalPitchAdjustment: Float = 1.0f,
        vocalRateAdjustment: Float = 1.0f
    ) {
        _lastAvatarResponse.value = text
        currentExpression = expression
        val utteranceId = "utt_${System.currentTimeMillis()}"

        val assistantTurn = ConversationTurn(
            role = "assistant",
            content = text,
            language = _settings.value.languageMode.code,
            evidenceLevel = evidenceClassification,
            timestamp = System.currentTimeMillis(),
            llmLatencyMs = llmLatency,
            toolCallBadge = toolBadge
        )
        _conversationTurns.value = _conversationTurns.value + assistantTurn

        viewModelScope.launch {
            repository.saveConversationTurn(sessionId, assistantTurn)
            repository.logAudit(
                sessionId = sessionId,
                eventType = "Response Generated",
                details = "Evidence: ${evidenceClassification.name}${if (toolBadge != null) ", Tool: $toolBadge" else ""}, Expression: ${expression.labelEn}, PitchAdj: ${"%.2f".format(vocalPitchAdjustment)}x, Text: \"$text\"",
                status = OperationStatus.SUCCEEDED,
                latencyMs = llmLatency
            )
        }

        if (_settings.value.isMuted) {
            _avatarState.value = AvatarState.IDLE
            onAvatarFinishedSpeaking()
            return
        }

        // Immediately animate the avatar visually so response is lively and interactive
        _avatarState.value = AvatarState.SPEAKING
        startLipSyncSpeechEnvelope()

        // Fallback visual speech duration for cloud emulator or devices with quiet/unsupported TTS
        fallbackSpeechJob?.cancel()
        val speechDurationMs = (text.length * 65L).coerceIn(2200L, 7500L)
        fallbackSpeechJob = viewModelScope.launch {
            delay(speechDurationMs)
            if (_avatarState.value == AvatarState.SPEAKING) {
                onAvatarFinishedSpeaking()
            }
        }

        val ttsStartTime = System.currentTimeMillis()

        // Dynamically adjusted vocal pitch and speed based on user's emotional input
        val adjustedPitch = (_settings.value.voicePitch * vocalPitchAdjustment).coerceIn(0.70f, 1.45f)
        val adjustedRate = (_settings.value.voiceSpeed * vocalRateAdjustment).coerceIn(0.70f, 1.40f)

        ttsProvider.speak(
            utteranceId = utteranceId,
            text = text,
            language = _settings.value.languageMode,
            voiceProfile = _selectedVoice.value,
            rateMultiplier = adjustedRate,
            pitchMultiplier = adjustedPitch,
            listener = object : TtsListener {
                override fun onStart(utteranceId: String) {
                    _avatarState.value = AvatarState.SPEAKING
                    startLipSyncSpeechEnvelope()
                    viewModelScope.launch {
                        repository.logAudit(
                            sessionId = sessionId,
                            eventType = "TTS Started",
                            details = "Playback started for utterance $utteranceId",
                            status = OperationStatus.SUCCEEDED,
                            latencyMs = System.currentTimeMillis() - ttsStartTime
                        )
                    }
                }

                override fun onRangeStart(utteranceId: String, start: Int, end: Int) {
                    // Word boundary hit: boost phoneme amplitude
                    _speechAudioAmplitude.value = 0.65f
                }

                override fun onDone(utteranceId: String) {
                    onAvatarFinishedSpeaking()
                    viewModelScope.launch {
                        repository.logAudit(
                            sessionId = sessionId,
                            eventType = "TTS Finished",
                            details = "Completed speech playback",
                            status = OperationStatus.SUCCEEDED
                        )
                    }
                }

                override fun onError(utteranceId: String, errorMsg: String) {
                    // Note: Do not abort lip sync immediately so visual animation continues
                    viewModelScope.launch {
                        repository.logAudit(
                            sessionId = sessionId,
                            eventType = "TTS Notice",
                            details = "Speech audio notice: $errorMsg",
                            status = OperationStatus.FAILED
                        )
                    }
                }
            }
        )
    }

    /**
     * Called when avatar finishes speaking (either from TTS onDone or fallback timer).
     * If Live Voice Call mode is active, smoothly restarts listening so conversation flows
     * continuously back and forth without user intervention.
     */
    private fun onAvatarFinishedSpeaking() {
        fallbackSpeechJob?.cancel()
        stopLipSyncSpeechEnvelope()
        _avatarState.value = AvatarState.IDLE
        currentExpression = FacialExpression.NEUTRAL

        if (_isLiveCallActive.value) {
            liveCallLoopJob?.cancel()
            liveCallLoopJob = viewModelScope.launch {
                // Natural human conversational turn-taking pause (approx 350ms)
                delay(350)
                if (_isLiveCallActive.value && _avatarState.value == AvatarState.IDLE) {
                    startListening()
                }
            }
        }
    }

    private fun startLipSyncSpeechEnvelope() {
        speechEnvelopeJob?.cancel()
        speechEnvelopeJob = viewModelScope.launch {
            var step = 0
            while (isActive && _avatarState.value == AvatarState.SPEAKING) {
                // Lifelike syllable cadence: bursts of vowel energy followed by brief consonant dip
                val syllablePulse = (kotlin.math.sin(step * 0.75).toFloat() * 0.5f + 0.5f)
                val phonemeJitter = (kotlin.math.sin(step * 1.85).toFloat() * 0.22f)
                val stressMod = if (step % 10 < 4) 0.85f else 0.55f
                val dynamicAmp = ((syllablePulse * stressMod) + phonemeJitter).coerceIn(0.12f, 0.95f)
                _speechAudioAmplitude.value = dynamicAmp
                step++
                delay(45) // ~22 updates/sec matching human syllable articulation
            }
        }
    }

    private fun stopLipSyncSpeechEnvelope() {
        speechEnvelopeJob?.cancel()
        speechEnvelopeJob = null
        _speechAudioAmplitude.value = 0f
    }

    fun selectLanguage(language: LanguageMode) {
        _settings.value = _settings.value.copy(languageMode = language)
        viewModelScope.launch {
            repository.logAudit(
                sessionId = sessionId,
                eventType = "Language Selected",
                details = "Switched to ${language.displayName}",
                status = OperationStatus.SUCCEEDED
            )
        }
    }

    fun selectCharacter(characterId: String) {
        val char = AvailableCharacters.getCharacter(characterId)
        _selectedCharacter.value = char
        _settings.value = _settings.value.copy(selectedCharacterId = characterId)

        // Adjust voice gender to match character gender
        val voices = AvailableCharacters.getVoicesForGender(char.gender)
        if (voices.none { it.id == _selectedVoice.value.id }) {
            _selectedVoice.value = voices.first()
            _settings.value = _settings.value.copy(selectedVoiceId = voices.first().id)
        }

        viewModelScope.launch {
            repository.logAudit(
                sessionId = sessionId,
                eventType = "Character Selected",
                details = "Selected ${char.nameEn} (${char.gender.name})",
                status = OperationStatus.SUCCEEDED
            )
        }
    }

    fun selectVoice(voiceId: String) {
        val voice = AvailableCharacters.getVoice(voiceId)
        _selectedVoice.value = voice
        _settings.value = _settings.value.copy(selectedVoiceId = voiceId)
    }

    fun setPersona(persona: PersonaStyle) {
        _selectedPersona.value = persona
    }

    fun setVoiceSpeed(speed: Float) {
        _settings.value = _settings.value.copy(voiceSpeed = speed)
    }

    fun setVoicePitch(pitch: Float) {
        _settings.value = _settings.value.copy(voicePitch = pitch)
    }

    fun toggleCodeSwitching(enabled: Boolean) {
        _settings.value = _settings.value.copy(allowCodeSwitching = enabled)
    }

    fun toggleUserCamera(enabled: Boolean) {
        _settings.value = _settings.value.copy(userCameraEnabled = enabled)
        viewModelScope.launch {
            repository.logAudit(
                sessionId = sessionId,
                eventType = "Camera Permission Toggle",
                details = if (enabled) "User enabled camera with explicit consent" else "User disabled camera",
                status = OperationStatus.SUCCEEDED
            )
        }
    }

    fun toggleMute() {
        val newMuted = !_settings.value.isMuted
        _settings.value = _settings.value.copy(isMuted = newMuted)
        if (newMuted && _avatarState.value == AvatarState.SPEAKING) {
            ttsProvider.stop()
            stopLipSyncSpeechEnvelope()
            _avatarState.value = AvatarState.IDLE
        }
    }

    fun previewVoice(voice: VoiceProfile) {
        val sampleText = if (_settings.value.languageMode == LanguageMode.ARABIC) {
            "مرحباً بك، هذا نموذج للصوت الطبيعي لشخصيتك الافتراضية."
        } else {
            "Hello, this is a sample of the natural voice for your avatar."
        }
        ttsProvider.speak(
            utteranceId = "prev_${System.currentTimeMillis()}",
            text = sampleText,
            language = _settings.value.languageMode,
            voiceProfile = voice,
            rateMultiplier = _settings.value.voiceSpeed,
            pitchMultiplier = _settings.value.voicePitch,
            listener = object : TtsListener {
                override fun onStart(utteranceId: String) {}
                override fun onRangeStart(utteranceId: String, start: Int, end: Int) {}
                override fun onDone(utteranceId: String) {}
                override fun onError(utteranceId: String, errorMsg: String) {}
            }
        )
    }

    // --- Memory Management ---
    fun addMemory(key: String, value: String, category: String = "fact") {
        viewModelScope.launch {
            repository.saveMemory(key, value, category)
            repository.logAudit(
                sessionId = sessionId,
                eventType = "Memory Added",
                details = "Saved fact: $key = $value",
                status = OperationStatus.SUCCEEDED
            )
        }
    }

    fun updateMemory(memory: PersistentMemoryEntity) {
        viewModelScope.launch {
            repository.updateMemory(memory)
            repository.logAudit(
                sessionId = sessionId,
                eventType = "Memory Updated",
                details = "Updated memory id=${memory.id}",
                status = OperationStatus.SUCCEEDED
            )
        }
    }

    fun deleteMemory(id: Long) {
        viewModelScope.launch {
            repository.deleteMemoryById(id)
            repository.logAudit(
                sessionId = sessionId,
                eventType = "Memory Deleted",
                details = "Deleted memory id=$id",
                status = OperationStatus.SUCCEEDED
            )
        }
    }

    fun clearAllMemories() {
        viewModelScope.launch {
            repository.clearMemories()
            repository.logAudit(
                sessionId = sessionId,
                eventType = "Memory Cleared",
                details = "Cleared all persistent memories",
                status = OperationStatus.SUCCEEDED
            )
        }
    }

    fun getAvailableLocalFiles(): List<com.example.engine.genai.LocalFileAccessFunctionHandler.LocalFileInfo> {
        return (llmProvider as? com.example.engine.ConversationEngine)?.genAiTools?.localFileHandler?.listLocalFiles() ?: emptyList()
    }

    fun writeLocalFile(name: String, content: String): Boolean {
        return (llmProvider as? com.example.engine.ConversationEngine)?.genAiTools?.localFileHandler?.writeLocalFile(name, content) ?: false
    }

    fun executeGoogleSearchAction(query: String) {
        val prompt = if (_settings.value.languageMode == LanguageMode.ARABIC) {
            "ابحث في جوجل عن $query"
        } else {
            "Search Google for $query"
        }
        processUserSpeech(prompt)
    }

    fun executeFileInspectionAction(fileName: String) {
        val prompt = if (_settings.value.languageMode == LanguageMode.ARABIC) {
            "اقرأ ملف $fileName وقدم لي خلاصة وافية"
        } else {
            "Read local file $fileName and provide an executive summary"
        }
        processUserSpeech(prompt)
    }

    fun clearHistory() {
        viewModelScope.launch {
            repository.clearHistory()
            _conversationTurns.value = emptyList()
        }
    }

    fun clearAuditLogs() {
        viewModelScope.launch {
            repository.clearLogs()
        }
    }

    fun clearError() {
        _errorMessage.value = null
    }

    override fun onCleared() {
        super.onCleared()
        animationLoopJob?.cancel()
        speechEnvelopeJob?.cancel()
        fallbackSpeechJob?.cancel()
        audioRecorder.stopMonitoring()
        asrProvider.destroy()
        ttsProvider.shutdown()
    }
}
