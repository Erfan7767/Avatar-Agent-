package com.example.model

import androidx.annotation.DrawableRes
import com.example.R

enum class AvatarState(val labelEn: String, val labelAr: String) {
    IDLE("Idle", "في الانتظار"),
    LISTENING("Listening...", "أستمع إليك..."),
    THINKING("Thinking...", "أفكر في الرد..."),
    SPEAKING("Speaking...", "أتحدث..."),
    INTERRUPTED("Interrupted", "تمت المقاطعة"),
    PAUSED("Paused", "متوقف مؤقتاً"),
    ERROR("Error", "حدث خطأ"),
    DISCONNECTED("Session Ended", "انتهت الجلسة")
}

enum class LanguageMode(val code: String, val displayName: String, val nativeName: String) {
    ARABIC("ar", "Arabic", "العربية"),
    ENGLISH("en", "English", "English")
}

enum class AvatarGender(val labelEn: String, val labelAr: String) {
    FEMALE("Female", "أنثى"),
    MALE("Male", "ذكر")
}

enum class PersonaStyle(val titleEn: String, val titleAr: String, val promptDescription: String) {
    WORLD_CLASS_EXPERT("World-Class Expert", "خبير عالمي رفيع المستوى", "A world-class, top-tier professional expert with profound wisdom, eloquence, warmth, and natural human conversational mastery."),
    FRIENDLY("Friendly", "ودود", "Warm, approachable, encouraging, and empathetic."),
    PROFESSIONAL("Professional", "مهني ورسمي", "Clear, concise, polished, and structured."),
    TEACHER("Teacher", "معلم ومرشد", "Patient, explanatory, educational, and structured."),
    CONVERSATION_PARTNER("Partner", "شريك حوار", "Engaging, inquisitive, active listener, and reflective."),
    TECHNICAL_EXPERT("Technical Expert", "خبير تقني", "Precise, analytical, logical, and evidence-focused."),
    CASUAL("Casual", "عفوي وبسيط", "Relaxed, natural, colloquial where appropriate, and warm."),
    FORMAL("Formal", "فصيح ورسمي", "High formality, disciplined vocabulary, and dignified tone.")
}

enum class FacialExpression(val labelEn: String, val labelAr: String) {
    NEUTRAL("Neutral", "هادئ"),
    GENTLE_SMILE("Gentle Smile", "ابتسامة خفيفة"),
    WARM_SMILE("Warm Smile", "ابتسامة دافئة"),
    THOUGHTFUL_ATTENTIVE("Thoughtful", "انتباه وتفكير"),
    SERIOUS_FOCUSED("Serious", "جاد ومحايد"),
    LISTENING_NOD("Listening", "تأكيد واستماع")
}

enum class EvidenceClassification {
    VERIFIED,
    SUPPORTED,
    UNCERTAIN,
    UNKNOWN
}

enum class OperationStatus {
    REQUESTED,
    STARTED,
    SUCCEEDED,
    FAILED,
    CANCELLED
}

data class AvatarCharacter(
    val id: String,
    val nameEn: String,
    val nameAr: String,
    val gender: AvatarGender,
    val defaultPersona: PersonaStyle,
    val greetingEn: String,
    val greetingAr: String,
    val descriptionEn: String,
    val descriptionAr: String,
    @DrawableRes val avatarResId: Int
)

data class VoiceProfile(
    val id: String,
    val nameEn: String,
    val nameAr: String,
    val gender: AvatarGender,
    val pitch: Float = 1.0f,
    val rate: Float = 1.0f,
    val descriptionEn: String,
    val descriptionAr: String
)

data class VisemeFrame(
    val jawOpen: Float = 0f,         // 0f (closed) to 1f (fully open)
    val mouthSpread: Float = 0f,     // 0f (narrow) to 1f (wide)
    val mouthO: Float = 0f,          // 0f (flat) to 1f (pursed/rounded)
    val eyelidOpen: Float = 1f,      // 1f (open) to 0f (blink closed)
    val gazeX: Float = 0f,           // -1f (left) to 1f (right)
    val gazeY: Float = 0f,           // -1f (up) to 1f (down)
    val headTilt: Float = 0f,        // degrees tilt (-3f to +3f)
    val headNod: Float = 0f,         // degrees nod (-2f to +2f)
    val smile: Float = 0.2f,         // 0f (straight) to 1f (broad smile)
    val expression: FacialExpression = FacialExpression.NEUTRAL
)

data class ToolCallInfo(
    val toolName: String,
    val toolIcon: String,
    val queryOrArg: String,
    val resultPreview: String,
    val timestamp: Long = System.currentTimeMillis()
)

data class ConversationTurn(
    val id: Long = 0,
    val role: String, // "user" or "assistant" or "system"
    val content: String,
    val language: String,
    val evidenceLevel: EvidenceClassification = EvidenceClassification.SUPPORTED,
    val timestamp: Long = System.currentTimeMillis(),
    val asrLatencyMs: Long = 0,
    val llmLatencyMs: Long = 0,
    val ttsLatencyMs: Long = 0,
    val renderLatencyMs: Long = 0,
    val toolCallBadge: String? = null
)

data class AuditLogEntry(
    val id: Long = 0,
    val timestamp: Long = System.currentTimeMillis(),
    val eventType: String,
    val details: String,
    val status: OperationStatus = OperationStatus.SUCCEEDED,
    val latencyMs: Long = 0
)

data class AppSettings(
    val languageMode: LanguageMode = LanguageMode.ARABIC,
    val allowCodeSwitching: Boolean = false,
    val selectedCharacterId: String = "elena_female",
    val selectedVoiceId: String = "female_voice_1",
    val voiceSpeed: Float = 1.0f,
    val voicePitch: Float = 1.0f, // 0.75f (deep/rich tone) to 1.35f (light/high pitch)
    val speechSensitivity: String = "NORMAL", // LOW, NORMAL, HIGH
    val animationLevel: String = "BALANCED", // SUBTLE, BALANCED, DYNAMIC
    val expressionLevel: String = "NATURAL", // LOW, NATURAL, VIVID
    val persistentMemoryEnabled: Boolean = true,
    val userCameraEnabled: Boolean = false,
    val isMuted: Boolean = false,
    val autoContinuousVoice: Boolean = true // Hands-free real-time continuous voice conversation like talking to a real human
)

object AvailableCharacters {
    val CHARACTERS = listOf(
        AvatarCharacter(
            id = "elena_female",
            nameEn = "Elena",
            nameAr = "إيلينا",
            gender = AvatarGender.FEMALE,
            defaultPersona = PersonaStyle.WORLD_CLASS_EXPERT,
            greetingEn = "Greetings. I am Elena, your world-class expert and AI conversation partner. I am ready for our real-time discussion. What is on your mind?",
            greetingAr = "أهلاً بك. أنا إيلينا، خبيرتك المحترفة وشريكتك في الحوار المباشر بالوقت الفعلي. يسعدني التواصل معك، ما الذي يشغل تفكيرك ونود مناقشته اليوم؟",
            descriptionEn = "World-class expert with captivating elegance, piercing blue eyes, blonde hair, and engaging natural eloquence.",
            descriptionAr = "خبيرة عالمية رفيعة المستوى بملامح شقراء دقيقة وعيون زرقاء وحوار واقعي فصيح ومتقن.",
            avatarResId = R.drawable.img_avatar_blonde
        ),
        AvatarCharacter(
            id = "sarah_female",
            nameEn = "Sarah",
            nameAr = "سارة",
            gender = AvatarGender.FEMALE,
            defaultPersona = PersonaStyle.WORLD_CLASS_EXPERT,
            greetingEn = "Hello. I am Sarah, your top-tier expert advisor. I am listening in real-time, how may I assist you today?",
            greetingAr = "مرحباً بك. أنا سارة، مستشارتك وخبيرتك المحترفة عالمياً. أستمع إليك مباشرة بالوقت الفعلي، كيف يمكنني مساعدتك وإفادتك اليوم؟",
            descriptionEn = "World-class professional and empathetic expert with deep knowledge and warm natural presence.",
            descriptionAr = "خبيرة محترفة عالمياً، دافئة وذكية تتفاعل بأسلوب علمي راقٍ ومريح.",
            avatarResId = R.drawable.img_avatar_female
        ),
        AvatarCharacter(
            id = "omar_male",
            nameEn = "Omar",
            nameAr = "عمر",
            gender = AvatarGender.MALE,
            defaultPersona = PersonaStyle.WORLD_CLASS_EXPERT,
            greetingEn = "Greetings. I am Omar, your world-class expert and advisor. I am here for our direct conversation. What would you like to explore?",
            greetingAr = "تحياتي الطيبة. أنا عمر، خبيرك ومستشارك رفيع المستوى. يسعدني جداً حديثنا الصوتي المباشر، ما الموضوع الذي تود استكشافه وتطويره اليوم؟",
            descriptionEn = "Top-tier world-class expert: poised, profoundly knowledgeable, articulate, and insightful.",
            descriptionAr = "خبير عالمي متزن، واسع المعرفة وفصيح البيان يقدم تحليلات ورؤى استراتيجية عميقة.",
            avatarResId = R.drawable.img_avatar_male
        )
    )

    val VOICES = listOf(
        VoiceProfile(
            id = "female_voice_1",
            nameEn = "Elena - Female Voice 1 (Melodic & Charming)",
            nameAr = "صوت أنثوي 1 - إلينا (عذب وجذاب)",
            gender = AvatarGender.FEMALE,
            pitch = 1.18f,
            rate = 0.96f,
            descriptionEn = "Beautiful, sweet feminine timbre with gentle intonation and clear melody.",
            descriptionAr = "نبرة أنثوية رقيقة وعذبة بنعومة طبيعية ومخارج حروف واضحة ومريحة للأذن."
        ),
        VoiceProfile(
            id = "female_voice_2",
            nameEn = "Sarah - Female Voice 2 (Soft & Elegant)",
            nameAr = "صوت أنثوي 2 - سارة (ناعم وأنيق)",
            gender = AvatarGender.FEMALE,
            pitch = 1.12f,
            rate = 0.94f,
            descriptionEn = "Silky, warm and charming voice with calm and sophisticated cadence.",
            descriptionAr = "صوت حريري دافئ وراقي يمنح إحساساً بالأناقة والراحة النفسية."
        ),
        VoiceProfile(
            id = "female_voice_3",
            nameEn = "Lina - Female Voice 3 (Bright & Cheerful)",
            nameAr = "صوت أنثوي 3 - لينا (مشرق ومفعم بالحياة)",
            gender = AvatarGender.FEMALE,
            pitch = 1.25f,
            rate = 1.0f,
            descriptionEn = "Youthful, sparkling feminine delivery full of friendly radiance.",
            descriptionAr = "صوت أنثوي حيوي نضر ومشرق يفيض بالبهجة والود."
        ),
        VoiceProfile(
            id = "male_voice_1",
            nameEn = "Male Voice 1 (Balanced & Friendly)",
            nameAr = "صوت ذكوري 1 (متوازن وودود)",
            gender = AvatarGender.MALE,
            pitch = 0.95f,
            rate = 1.0f,
            descriptionEn = "Natural, approachable male voice with warm intonation.",
            descriptionAr = "صوت ذكوري طبيعي ونبرة محببة ومتزنة."
        ),
        VoiceProfile(
            id = "male_voice_2",
            nameEn = "Male Voice 2 (Deep & Authoritative)",
            nameAr = "صوت ذكوري 2 (عميق ورسمي)",
            gender = AvatarGender.MALE,
            pitch = 0.82f,
            rate = 0.98f,
            descriptionEn = "Resonant tone with firm, structured conversational rhythm.",
            descriptionAr = "نبرة عميقة وقوية ذات إيقاع منظم واحترافي."
        ),
        VoiceProfile(
            id = "male_voice_3",
            nameEn = "Male Voice 3 (Dynamic & Engaging)",
            nameAr = "صوت ذكوري 3 (حيوي ومتحمس)",
            gender = AvatarGender.MALE,
            pitch = 1.02f,
            rate = 1.08f,
            descriptionEn = "Energetic and crisp, ideal for active discussion and tech topics.",
            descriptionAr = "صوت حيوي وواضح ممتاز للموضوعات العلمية والتقنية."
        )
    )

    fun getCharacter(id: String): AvatarCharacter =
        CHARACTERS.find { it.id == id } ?: CHARACTERS.first()

    fun getVoice(id: String): VoiceProfile =
        VOICES.find { it.id == id } ?: VOICES.first()

    fun getVoicesForGender(gender: AvatarGender): List<VoiceProfile> =
        VOICES.filter { it.gender == gender }

    val allCharacters: List<AvatarCharacter> get() = CHARACTERS
}
