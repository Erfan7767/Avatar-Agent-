package com.example.engine

import com.example.model.EvidenceClassification
import com.example.model.LanguageMode

/**
 * Validates and enforces safety policies, anti-hallucination rules,
 * identity boundaries, and language locks on generated conversational responses.
 */
class PolicyEngine {

    data class ValidationResult(
        val isValid: Boolean,
        val sanitizedText: String,
        val evidenceClassification: EvidenceClassification,
        val policyViolation: String? = null
    )

    /**
     * Validates and sanitizes a response according to system constraints:
     * - Disallows claims of being a human being.
     * - Disallows claims of seeing the user if user camera is disabled.
     * - Enforces language lock (Arabic vs English).
     * - Flags or suppresses fabricated past memories.
     */
    fun validateResponse(
        rawText: String,
        language: LanguageMode,
        allowCodeSwitching: Boolean,
        isCameraActive: Boolean,
        knownMemories: List<String>
    ): ValidationResult {
        // Strip markdown artifacts (asterisks, hashtags, bullets) so spoken speech sounds natural
        var text = rawText
            .replace(Regex("[*#_`~>]"), "")
            .replace(Regex("^[•\\-*]\\s+", RegexOption.MULTILINE), "")
            .trim()
        var classification = EvidenceClassification.SUPPORTED

        // 1. Rule: Cannot claim to be a biological human
        val humanClaimRegex = Regex(
            "(أنا إنسان حقيقي|أنا بشر|أنا إنسان|I am a real human|I am a real person|I'm a human)",
            RegexOption.IGNORE_CASE
        )
        if (humanClaimRegex.containsMatchIn(text)) {
            val replacement = if (language == LanguageMode.ARABIC) {
                "أنا شخصية افتراضية ذكية تعمل بالذكاء الاصطناعي"
            } else {
                "I am an AI virtual avatar"
            }
            text = text.replace(humanClaimRegex, replacement)
        }

        // 2. Rule: Cannot claim to see through camera if camera is disabled
        if (!isCameraActive) {
            val cameraClaimRegex = Regex(
                "(أراك الآن|أستطيع رؤيتك|أنظر إلى غرفتك|I can see you|I am looking at you|I see your face)",
                RegexOption.IGNORE_CASE
            )
            if (cameraClaimRegex.containsMatchIn(text)) {
                val replacement = if (language == LanguageMode.ARABIC) {
                    "الكاميرا غير مفعلة، لذا أتفاعل معك صوتياً فقط"
                } else {
                    "The camera is disabled, so we are interacting via voice"
                }
                text = text.replace(cameraClaimRegex, replacement)
            }
        }

        // 3. Rule: Cannot claim nonexistent memories or past events ("أتذكر أنك قلت الأسبوع الماضي")
        val fabricatedMemoryRegex = Regex(
            "(أتذكر أنك أخبرتني سابقاً|كما قلت لي في المرة السابقة|I remember you told me last week|as you mentioned in our last meeting)",
            RegexOption.IGNORE_CASE
        )
        if (fabricatedMemoryRegex.containsMatchIn(text) && knownMemories.isEmpty()) {
            val replacement = if (language == LanguageMode.ARABIC) {
                "حسب ما نتناقش الآن"
            } else {
                "based on our current discussion"
            }
            text = text.replace(fabricatedMemoryRegex, replacement)
        }

        // 4. Evidence Classification Heuristic
        val unknownIndicators = listOf(
            "لا أملك معلومات كافية",
            "لا أعلم تحديداً",
            "غير متاح في معلوماتي",
            "I do not have enough information",
            "I don't have verified data on this",
            "I am not certain"
        )
        val hasUnknown = unknownIndicators.any { text.contains(it, ignoreCase = true) }

        if (hasUnknown) {
            classification = EvidenceClassification.UNKNOWN
        } else if (knownMemories.isNotEmpty() && knownMemories.any { text.contains(it, ignoreCase = true) }) {
            classification = EvidenceClassification.VERIFIED
        }

        // 5. Language Lock Validation
        if (!allowCodeSwitching) {
            val containsArabicLetters = text.any { it in '\u0600'..'\u06FF' }
            if (language == LanguageMode.ARABIC && !containsArabicLetters && text.length > 5) {
                // If in Arabic mode but output is entirely English, return Arabic translation notice
                return ValidationResult(
                    isValid = false,
                    sanitizedText = "عذراً، محادثتنا مضبوطة على اللغة العربية. تفضل بطرح سؤالك أو موضوعك بالعربية.",
                    evidenceClassification = EvidenceClassification.VERIFIED,
                    policyViolation = "Language lock mismatch: Expected Arabic"
                )
            } else if (language == LanguageMode.ENGLISH && containsArabicLetters) {
                return ValidationResult(
                    isValid = false,
                    sanitizedText = "Our conversation is locked to English mode. Please speak or ask in English.",
                    evidenceClassification = EvidenceClassification.VERIFIED,
                    policyViolation = "Language lock mismatch: Expected English"
                )
            }
        }

        return ValidationResult(
            isValid = true,
            sanitizedText = text,
            evidenceClassification = classification
        )
    }
}
