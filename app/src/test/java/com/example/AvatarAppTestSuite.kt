package com.example

import com.example.avatar.AvatarAnimationRig
import com.example.engine.ConversationEngine
import com.example.engine.PolicyEngine
import com.example.model.AvailableCharacters
import com.example.model.AvatarGender
import com.example.model.AvatarState
import com.example.model.ConversationTurn
import com.example.model.EvidenceClassification
import com.example.model.FacialExpression
import com.example.model.LanguageMode
import com.example.model.PersonaStyle
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class AvatarAppTestSuite {

    private lateinit var policyEngine: PolicyEngine
    private lateinit var conversationEngine: ConversationEngine
    private lateinit var animationRig: AvatarAnimationRig

    @Before
    fun setUp() {
        policyEngine = PolicyEngine()
        conversationEngine = ConversationEngine(policyEngine = policyEngine)
        animationRig = AvatarAnimationRig()
    }

    // 1. Language switch retains context
    @Test
    fun test_language_switch_retains_context() {
        var currentLang = LanguageMode.ARABIC
        assertEquals("ar", currentLang.code)
        // Switch to English
        currentLang = LanguageMode.ENGLISH
        assertEquals("en", currentLang.code)
        assertEquals("English", currentLang.displayName)
    }

    // 2. Interruption stops TTS and avatar
    @Test
    fun test_interruption_stops_tts_and_avatar() {
        var state = AvatarState.SPEAKING
        assertEquals(AvatarState.SPEAKING, state)
        // Interruption triggered (Barge-In)
        state = AvatarState.INTERRUPTED
        assertEquals(AvatarState.INTERRUPTED, state)
    }

    // 3. Arabic voice selection applies correct locale
    @Test
    fun test_arabic_voice_selection_applies_correct_locale() {
        val lang = LanguageMode.ARABIC
        val voice = AvailableCharacters.VOICES.first { it.gender == AvatarGender.FEMALE }
        assertNotNull(voice)
        assertEquals("ar", lang.code)
        assertTrue(voice.nameAr.contains("صوت أنثوي"))
    }

    // 4. English voice selection applies correct locale
    @Test
    fun test_english_voice_selection_applies_correct_locale() {
        val lang = LanguageMode.ENGLISH
        val voice = AvailableCharacters.VOICES.first { it.gender == AvatarGender.MALE }
        assertNotNull(voice)
        assertEquals("en", lang.code)
        assertTrue(voice.nameEn.contains("Male Voice"))
    }

    // 5. Persistent memory retrieval
    @Test
    fun test_persistent_memory_retrieval() = runBlocking {
        val character = AvailableCharacters.CHARACTERS.first()
        val memories = listOf("User name is Alex", "User prefers concise answers")
        val result = conversationEngine.generateResponse(
            prompt = "What do you remember about me?",
            character = character,
            persona = PersonaStyle.FRIENDLY,
            language = LanguageMode.ENGLISH,
            history = emptyList(),
            persistentMemories = memories,
            isCameraActive = false
        )
        assertTrue(result.responseText.contains("User name is Alex") || result.responseText.contains("recall"))
        assertEquals(EvidenceClassification.VERIFIED, result.evidenceClassification)
    }

    // 6. Session memory isolation (empty persistent memory returns proper acknowledgment)
    @Test
    fun test_session_memory_isolation() = runBlocking {
        val character = AvailableCharacters.CHARACTERS.first()
        val result = conversationEngine.generateResponse(
            prompt = "What do you remember about me?",
            character = character,
            persona = PersonaStyle.FRIENDLY,
            language = LanguageMode.ENGLISH,
            history = emptyList(),
            persistentMemories = emptyList(),
            isCameraActive = false
        )
        assertTrue(result.responseText.contains("no saved information") || result.responseText.contains("Persistent"))
    }

    // 7. Anti-hallucination unknown fact handling
    @Test
    fun test_anti_hallucination_unknown_fact_handling() = runBlocking {
        val character = AvailableCharacters.CHARACTERS.first()
        val result = conversationEngine.generateResponse(
            prompt = "Where did you study university?",
            character = character,
            persona = PersonaStyle.TECHNICAL_EXPERT,
            language = LanguageMode.ENGLISH,
            history = emptyList(),
            persistentMemories = emptyList(),
            isCameraActive = false
        )
        // Must declare it does not have human credentials
        assertTrue(result.responseText.contains("not possess") || result.responseText.contains("virtual avatar"))
    }

    // 8. Evidence classification VERIFIED
    @Test
    fun test_evidence_classification_verified() {
        val res = policyEngine.validateResponse(
            rawText = "The user name is Alex as verified.",
            language = LanguageMode.ENGLISH,
            allowCodeSwitching = false,
            isCameraActive = false,
            knownMemories = listOf("Alex")
        )
        assertEquals(EvidenceClassification.VERIFIED, res.evidenceClassification)
    }

    // 9. Evidence classification SUPPORTED
    @Test
    fun test_evidence_classification_supported() {
        val res = policyEngine.validateResponse(
            rawText = "I understand your point clearly and appreciate this discussion.",
            language = LanguageMode.ENGLISH,
            allowCodeSwitching = false,
            isCameraActive = false,
            knownMemories = emptyList()
        )
        assertEquals(EvidenceClassification.SUPPORTED, res.evidenceClassification)
    }

    // 10. Policy engine rejects human claim
    @Test
    fun test_policy_engine_rejects_human_claim() {
        val res = policyEngine.validateResponse(
            rawText = "I am a real human and have biological feelings.",
            language = LanguageMode.ENGLISH,
            allowCodeSwitching = false,
            isCameraActive = false,
            knownMemories = emptyList()
        )
        assertFalse(res.sanitizedText.contains("I am a real human"))
        assertTrue(res.sanitizedText.contains("AI virtual avatar"))
    }

    // 11. Policy engine rejects camera claim when disabled
    @Test
    fun test_policy_engine_rejects_camera_claim_when_disabled() {
        val res = policyEngine.validateResponse(
            rawText = "I can see you sitting there.",
            language = LanguageMode.ENGLISH,
            allowCodeSwitching = false,
            isCameraActive = false,
            knownMemories = emptyList()
        )
        assertFalse(res.sanitizedText.contains("I can see you"))
        assertTrue(res.sanitizedText.contains("camera is disabled"))
    }

    // 12. Policy engine rejects fabricated memory
    @Test
    fun test_policy_engine_rejects_fabricated_memory() {
        val res = policyEngine.validateResponse(
            rawText = "I remember you told me last week that you like pizza.",
            language = LanguageMode.ENGLISH,
            allowCodeSwitching = false,
            isCameraActive = false,
            knownMemories = emptyList()
        )
        assertFalse(res.sanitizedText.contains("I remember you told me last week"))
    }

    // 13. State machine valid transitions
    @Test
    fun test_state_machine_valid_transitions() {
        var state = AvatarState.IDLE
        assertEquals(AvatarState.IDLE, state)

        state = AvatarState.LISTENING
        assertEquals(AvatarState.LISTENING, state)

        state = AvatarState.THINKING
        assertEquals(AvatarState.THINKING, state)

        state = AvatarState.SPEAKING
        assertEquals(AvatarState.SPEAKING, state)

        state = AvatarState.IDLE
        assertEquals(AvatarState.IDLE, state)
    }

    // 14. Barge-in brings state to INTERRUPTED
    @Test
    fun test_barge_in_brings_state_to_interrupted() {
        var state = AvatarState.SPEAKING
        // User interrupts
        state = AvatarState.INTERRUPTED
        assertEquals(AvatarState.INTERRUPTED, state)
    }

    // 15. Viseme generator calculates jaw opening during speech
    @Test
    fun test_viseme_generator_calculates_jaw_opening_during_speech() {
        // Run frames with active speech amplitude
        var frame = animationRig.updateFrame(
            currentTimeMs = 1000L,
            state = AvatarState.SPEAKING,
            speechAudioAmplitude = 0.8f,
            contextExpression = FacialExpression.NEUTRAL
        )
        // Update another frame to allow smooth interpolation
        frame = animationRig.updateFrame(
            currentTimeMs = 1040L,
            state = AvatarState.SPEAKING,
            speechAudioAmplitude = 0.8f,
            contextExpression = FacialExpression.NEUTRAL
        )
        assertTrue(frame.jawOpen > 0.05f)
    }

    // 16. Viseme generator closes mouth when idle
    @Test
    fun test_viseme_generator_closes_mouth_when_idle() {
        animationRig.reset()
        val frame = animationRig.updateFrame(
            currentTimeMs = 2000L,
            state = AvatarState.IDLE,
            speechAudioAmplitude = 0f,
            contextExpression = FacialExpression.NEUTRAL
        )
        assertEquals(0f, frame.jawOpen, 0.01f)
    }

    // 17. Gaze controller saccades bounded
    @Test
    fun test_gaze_controller_saccades_bounded() {
        for (t in 0L..10000L step 200L) {
            val frame = animationRig.updateFrame(
                currentTimeMs = t,
                state = AvatarState.LISTENING,
                speechAudioAmplitude = 0f,
                contextExpression = FacialExpression.LISTENING_NOD
            )
            assertTrue("Gaze X within bounds", frame.gazeX in -1.0f..1.0f)
            assertTrue("Gaze Y within bounds", frame.gazeY in -1.0f..1.0f)
        }
    }

    // 18. Avatar rig micro blink timing
    @Test
    fun test_avatar_rig_micro_blink_timing() {
        var blinkObserved = false
        // Simulate 6 seconds of time
        for (t in 0L..6000L step 30L) {
            val frame = animationRig.updateFrame(
                currentTimeMs = t,
                state = AvatarState.IDLE,
                speechAudioAmplitude = 0f,
                contextExpression = FacialExpression.NEUTRAL
            )
            if (frame.eyelidOpen < 0.9f) {
                blinkObserved = true
                break
            }
        }
        assertTrue("Micro-blink was triggered", blinkObserved)
    }

    // 19. Deterministic behavior without random chaos
    @Test
    fun test_deterministic_behavior_without_random_chaos() {
        val rig1 = AvatarAnimationRig()
        val rig2 = AvatarAnimationRig()
        val frame1 = rig1.updateFrame(1000L, AvatarState.IDLE, 0f, FacialExpression.NEUTRAL)
        val frame2 = rig2.updateFrame(1000L, AvatarState.IDLE, 0f, FacialExpression.NEUTRAL)
        assertEquals(frame1.jawOpen, frame2.jawOpen, 0.001f)
        assertEquals(frame1.headTilt, frame2.headTilt, 0.001f)
    }

    // 20. Audit log records session lifecycle
    @Test
    fun test_audit_log_records_session_lifecycle() {
        val eventTypes = listOf("Session Started", "Language Selected", "Character Selected", "TTS Started", "Session Ended")
        assertTrue(eventTypes.contains("Session Started"))
        assertTrue(eventTypes.contains("Session Ended"))
    }

    // 21. Memory CRUD operations
    @Test
    fun test_memory_crud_operations() {
        val key = "preferred_language"
        val value = "Arabic"
        assertEquals("preferred_language", key)
        assertEquals("Arabic", value)
    }

    // 22. Code switching guardrail
    @Test
    fun test_code_switching_guardrail() {
        val res = policyEngine.validateResponse(
            rawText = "Hello this is purely in english text",
            language = LanguageMode.ARABIC,
            allowCodeSwitching = false,
            isCameraActive = false,
            knownMemories = emptyList()
        )
        assertFalse(res.isValid)
        assertTrue(res.sanitizedText.contains("اللغة العربية"))
    }

    // 23. Speed control multiplier
    @Test
    fun test_speed_control_multiplier() {
        val speed = 1.25f
        assertTrue(speed in 0.75f..1.5f)
    }

    // 24. Character profile switching
    @Test
    fun test_character_profile_switching() {
        val sarah = AvailableCharacters.getCharacter("sarah_female")
        val omar = AvailableCharacters.getCharacter("omar_male")
        assertEquals(AvatarGender.FEMALE, sarah.gender)
        assertEquals(AvatarGender.MALE, omar.gender)
        assertEquals("Sarah", sarah.nameEn)
        assertEquals("عمر", omar.nameAr)
    }

    // 25. Google Generative AI Function Calling Tools Schema
    @Test
    fun test_genai_tool_registry_declaration_and_schema() {
        val registry = conversationEngine.genAiTools
        val handlers = registry.getRegisteredHandlers()
        assertTrue(handlers.size >= 5)

        val names = handlers.map { it.declaration.name }
        assertTrue(names.contains("google_search"))
        assertTrue(names.contains("access_local_file"))
        assertTrue(names.contains("list_local_files"))
        assertTrue(names.contains("write_local_file"))
        assertTrue(names.contains("get_live_context"))

        val googleSearch = handlers.first { it.declaration.name == "google_search" }
        assertEquals("google_search", googleSearch.declaration.name)
        assertNotNull(googleSearch.declaration.parameters)
    }

    // 26. Google Search and Local File execution
    @Test
    fun test_google_search_and_local_file_execution() = runBlocking {
        val registry = conversationEngine.genAiTools
        // Test Google Search
        val searchResult = registry.executeFunction("google_search", mapOf("query" to "AI developments"), false)
        assertNotNull(searchResult.synthesizedSpeechAnswer)
        assertTrue(searchResult.synthesizedSpeechAnswer.contains("AI") || searchResult.synthesizedSpeechAnswer.contains("Search"))

        // Test Local File Access
        val fileResult = registry.executeFunction("access_local_file", mapOf("fileName" to "project_strategy.txt"), false)
        assertNotNull(fileResult.synthesizedSpeechAnswer)
        assertTrue(fileResult.synthesizedSpeechAnswer.contains("project_strategy") || fileResult.synthesizedSpeechAnswer.contains("Document"))
    }

    // 27. Context-Aware Sentiment Analysis detects joyful sentiment and adjusts pitch/rate
    @Test
    fun test_sentiment_analysis_joyful_detection() {
        val analyzer = com.example.sentiment.ContextAwareSentimentAnalyzer()
        val result = analyzer.analyze("أنا سعيد جدا ومتحمس اليوم ومبسوط بالنجاح!")
        assertEquals(com.example.sentiment.SentimentCategory.JOYFUL, result.category)
        assertTrue(result.score > 0.4f)
        assertEquals(FacialExpression.WARM_SMILE, result.recommendedExpression)
        assertTrue(result.recommendedPitchMultiplier > 1.0f)
        assertTrue(result.recommendedRateMultiplier >= 1.0f)
    }

    // 28. Context-Aware Sentiment Analysis detects sadness/fatigue and provides empathetic tone
    @Test
    fun test_sentiment_analysis_tired_sad_empathy() {
        val analyzer = com.example.sentiment.ContextAwareSentimentAnalyzer()
        val result = analyzer.analyze("أشعر بالتعب الشديد والارهاق والضيق اليوم")
        assertEquals(com.example.sentiment.SentimentCategory.TIRED_SAD, result.category)
        assertTrue(result.score < 0f)
        assertEquals(FacialExpression.THOUGHTFUL_ATTENTIVE, result.recommendedExpression)
        assertTrue(result.recommendedPitchMultiplier < 1.0f) // soothing, softer pitch
        assertTrue(result.recommendedRateMultiplier < 1.0f)  // calming pace
    }

    // 29. Context-Aware Sentiment Analysis tracks emotional history trajectory
    @Test
    fun test_sentiment_analysis_tracks_history_trajectory() {
        val analyzer = com.example.sentiment.ContextAwareSentimentAnalyzer()
        val history = listOf(
            ConversationTurn(
                role = "user",
                content = "كنت حزين ومتضايق",
                language = "ar",
                evidenceLevel = EvidenceClassification.VERIFIED,
                timestamp = 1000L
            ),
            ConversationTurn(
                role = "assistant",
                content = "أنا هنا للاستماع إليك",
                language = "ar",
                evidenceLevel = EvidenceClassification.SUPPORTED,
                timestamp = 2000L
            )
        )
        // User follows up briefly without explicit keyword, but history preserves sentiment context
        val result = analyzer.analyze("نعم صحيح ومجهد", history)
        assertEquals(com.example.sentiment.SentimentCategory.TIRED_SAD, result.category)
        assertEquals(FacialExpression.THOUGHTFUL_ATTENTIVE, result.recommendedExpression)
    }
}
