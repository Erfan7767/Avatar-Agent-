package com.example.sentiment

import com.example.model.ConversationTurn
import com.example.model.FacialExpression
import java.util.Locale

/**
 * High-level emotional valence detected from sentiment analysis.
 */
enum class SentimentCategory(val labelEn: String, val labelAr: String, val emoji: String) {
    JOYFUL("Joyful", "مبتهج ومتحمس", "😊"),
    WARM_GRATITUDE("Grateful", "ممتن وودود", "🥰"),
    TIRED_SAD("Tired / Down", "مرهق أو حزين", "😔"),
    CURIOUS_QUESTIONING("Curious", "متسائل وباحث", "🤔"),
    SERIOUS_ANALYTICAL("Analytical", "جاد وتحليلي", "🧐"),
    NEUTRAL_CALM("Calm", "هادئ ومتزن", "😌")
}

/**
 * Output of the sentiment and emotional context evaluation.
 */
data class SentimentAnalysisResult(
    val category: SentimentCategory,
    val score: Float, // -1.0f (strongly negative/fatigued) to +1.0f (strongly positive/enthusiastic)
    val recommendedExpression: FacialExpression,
    val recommendedPitchMultiplier: Float, // dynamic vocal adjustment
    val recommendedRateMultiplier: Float,  // dynamic speech rate adjustment
    val emotionalSummaryEn: String,
    val emotionalSummaryAr: String
)

/**
 * Context-aware sentiment analysis engine that analyzes user input in real-time,
 * tracks conversation emotional trajectory over turns, and calculates fine-tuned
 * facial expressions and vocal tone parameters.
 */
class ContextAwareSentimentAnalyzer {

    // Positive / Joyful lexical patterns
    private val joyfulKeywordsAr = listOf(
        "سعيد", "فرحان", "مبسوط", "ممتاز", "رائع", "عظيم", "أحب", "متحمس", "أفضل", "جميل جدا",
        "مذهل", "أحسنت", "روعة", "فخور", "نجاح", "أفرحتني", "يوم جميل"
    )
    private val joyfulKeywordsEn = listOf(
        "happy", "glad", "joy", "excited", "awesome", "great", "excellent", "love", "wonderful",
        "fantastic", "amazing", "proud", "delighted", "superb", "brilliant", "success"
    )

    // Gratitude / Warmth patterns
    private val gratefulKeywordsAr = listOf(
        "شكرا", "أشكرك", "ممتن", "تسلم", "الله يعطيك العافية", "بارك الله فيك", "لطيف",
        "يسلمو", "حبيبي", "عزيزي", "مقدر لك", "مشكور", "جزاك الله خيرا"
    )
    private val gratefulKeywordsEn = listOf(
        "thank", "thanks", "grateful", "appreciate", "kind of you", "sweet", "dear", "warmly"
    )

    // Fatigue / Sadness / Overwhelm patterns
    private val tiredSadKeywordsAr = listOf(
        "تعبت", "مرهق", "طفشان", "حزين", "مكتئب", "متضايق", "خايف", "قلق", "صعب", "مشكلة",
        "يائس", "مجهد", "ألم", "معاناة", "ضيق", "إحباط", "أشعر بالتعب", "استنزاف"
    )
    private val tiredSadKeywordsEn = listOf(
        "tired", "exhausted", "sad", "unhappy", "depressed", "stressed", "anxious", "worried",
        "pain", "hurts", "difficult", "struggle", "burnout", "frustrated", "overwhelmed"
    )

    // Curious / Inquisitive patterns
    private val curiousKeywordsAr = listOf(
        "كيف", "لماذا", "ما رأيك", "هل يمكن", "اشرح لي", "علمني", "أريد أن أعرف", "غريب", "أتساءل", "ما السر"
    )
    private val curiousKeywordsEn = listOf(
        "how", "why", "what if", "explain", "curious", "wonder", "teach me", "tell me more", "reason"
    )

    // Serious / Analytical / Demanding patterns
    private val analyticalKeywordsAr = listOf(
        "حلل", "دراسة", "أرقام", "بيانات", "إحصاء", "بدقة", "مقارنة", "استراتيجية", "منهجية", "رسمي", "جدوى"
    )
    private val analyticalKeywordsEn = listOf(
        "analyze", "analysis", "data", "metrics", "strategy", "rigorous", "compare", "evaluate", "precise"
    )

    /**
     * Evaluates current user input along with conversation history to provide
     * a context-aware sentiment analysis result.
     */
    fun analyze(
        currentInput: String,
        history: List<ConversationTurn> = emptyList()
    ): SentimentAnalysisResult {
        val lower = currentInput.lowercase(Locale.ROOT).trim()

        var joyfulScore = 0
        var gratefulScore = 0
        var tiredSadScore = 0
        var curiousScore = 0
        var analyticalScore = 0

        // 1. Current text scan
        joyfulKeywordsAr.forEach { if (lower.contains(it)) joyfulScore += 3 }
        joyfulKeywordsEn.forEach { if (lower.contains(it)) joyfulScore += 3 }

        gratefulKeywordsAr.forEach { if (lower.contains(it)) gratefulScore += 3 }
        gratefulKeywordsEn.forEach { if (lower.contains(it)) gratefulScore += 3 }

        tiredSadKeywordsAr.forEach { if (lower.contains(it)) tiredSadScore += 4 }
        tiredSadKeywordsEn.forEach { if (lower.contains(it)) tiredSadScore += 4 }

        curiousKeywordsAr.forEach { if (lower.contains(it)) curiousScore += 2 }
        curiousKeywordsEn.forEach { if (lower.contains(it)) curiousScore += 2 }

        analyticalKeywordsAr.forEach { if (lower.contains(it)) analyticalScore += 2 }
        analyticalKeywordsEn.forEach { if (lower.contains(it)) analyticalScore += 2 }

        // 2. Track conversation trajectory from history (last 3 user turns weighted)
        val recentUserTurns = history.filter { it.role == "user" }.takeLast(3)
        var historyWeight = 1.0f
        for (turn in recentUserTurns.reversed()) {
            val hText = turn.content.lowercase(Locale.ROOT)
            joyfulKeywordsAr.forEach { if (hText.contains(it)) joyfulScore += (1 * historyWeight).toInt() }
            joyfulKeywordsEn.forEach { if (hText.contains(it)) joyfulScore += (1 * historyWeight).toInt() }

            gratefulKeywordsAr.forEach { if (hText.contains(it)) gratefulScore += (1 * historyWeight).toInt() }
            gratefulKeywordsEn.forEach { if (hText.contains(it)) gratefulScore += (1 * historyWeight).toInt() }

            tiredSadKeywordsAr.forEach { if (hText.contains(it)) tiredSadScore += (2 * historyWeight).toInt() }
            tiredSadKeywordsEn.forEach { if (hText.contains(it)) tiredSadScore += (2 * historyWeight).toInt() }

            curiousKeywordsAr.forEach { if (hText.contains(it)) curiousScore += (1 * historyWeight).toInt() }
            curiousKeywordsEn.forEach { if (hText.contains(it)) curiousScore += (1 * historyWeight).toInt() }

            analyticalKeywordsAr.forEach { if (hText.contains(it)) analyticalScore += (1 * historyWeight).toInt() }
            analyticalKeywordsEn.forEach { if (hText.contains(it)) analyticalScore += (1 * historyWeight).toInt() }

            historyWeight *= 0.5f // Diminish older history
        }

        // 3. Determine dominant sentiment category and vocal/expression adjustments
        return when {
            // Empathy for sadness / fatigue takes precedence
            tiredSadScore > 0 && tiredSadScore >= joyfulScore -> {
                SentimentAnalysisResult(
                    category = SentimentCategory.TIRED_SAD,
                    score = (-0.6f - (tiredSadScore * 0.05f)).coerceIn(-1.0f, -0.3f),
                    recommendedExpression = FacialExpression.THOUGHTFUL_ATTENTIVE,
                    recommendedPitchMultiplier = 0.95f, // Softer, warmer, comforting tone
                    recommendedRateMultiplier = 0.92f,  // Slower, calming pace
                    emotionalSummaryEn = "User seems weary or stressed; responding with gentle empathy and reassuring cadence.",
                    emotionalSummaryAr = "المستخدم يبدو مجهداً أو متعباً؛ الاستجابة بتعاطف رقيق ونبرة صوت دافئة ومطمئنة."
                )
            }
            joyfulScore > 0 && joyfulScore >= gratefulScore -> {
                SentimentAnalysisResult(
                    category = SentimentCategory.JOYFUL,
                    score = (0.5f + (joyfulScore * 0.08f)).coerceIn(0.4f, 1.0f),
                    recommendedExpression = FacialExpression.WARM_SMILE,
                    recommendedPitchMultiplier = 1.08f, // Slightly brighter, enthusiastic tone
                    recommendedRateMultiplier = 1.04f,  // Lively conversational tempo
                    emotionalSummaryEn = "User is energized and cheerful; mirroring positive excitement with radiant smile.",
                    emotionalSummaryAr = "المستخدم يشعر بالحماس والبهجة؛ التفاعل بابتسامة مشرقة ونبرة حيوية متفائلة."
                )
            }
            gratefulScore > 0 -> {
                SentimentAnalysisResult(
                    category = SentimentCategory.WARM_GRATITUDE,
                    score = 0.6f,
                    recommendedExpression = FacialExpression.GENTLE_SMILE,
                    recommendedPitchMultiplier = 1.03f, // Warm, gentle timbre
                    recommendedRateMultiplier = 0.98f,  // Natural relaxed flow
                    emotionalSummaryEn = "User expressed warm appreciation; mirroring gracious warmth and gentle smile.",
                    emotionalSummaryAr = "المستخدم يعبر عن التقدير والامتنان؛ التجاوب بنبرة عذبة وابتسامة لطيفة."
                )
            }
            curiousScore > analyticalScore && curiousScore > 0 -> {
                SentimentAnalysisResult(
                    category = SentimentCategory.CURIOUS_QUESTIONING,
                    score = 0.2f,
                    recommendedExpression = FacialExpression.LISTENING_NOD,
                    recommendedPitchMultiplier = 1.02f, // Inquisitive, engaging pitch
                    recommendedRateMultiplier = 1.0f,
                    emotionalSummaryEn = "User is actively inquiring; engaged listening nod with clear articulation.",
                    emotionalSummaryAr = "المستخدم يطرح تساؤلاً باهتمام؛ إيماءة استماع وانتباه مع وضوح الشرح."
                )
            }
            analyticalScore > 0 -> {
                SentimentAnalysisResult(
                    category = SentimentCategory.SERIOUS_ANALYTICAL,
                    score = 0.0f,
                    recommendedExpression = FacialExpression.SERIOUS_FOCUSED,
                    recommendedPitchMultiplier = 0.98f, // Focused, dignified tone
                    recommendedRateMultiplier = 0.98f,  // Deliberate articulate pace
                    emotionalSummaryEn = "User focuses on structured inquiry; focused composure and disciplined voice.",
                    emotionalSummaryAr = "المستخدم يركز على جانب تحليلي دقيق؛ وقار وانتباه ونبرة جادة واثقة."
                )
            }
            else -> {
                SentimentAnalysisResult(
                    category = SentimentCategory.NEUTRAL_CALM,
                    score = 0.0f,
                    recommendedExpression = FacialExpression.WARM_SMILE,
                    recommendedPitchMultiplier = 1.0f,
                    recommendedRateMultiplier = 1.0f,
                    emotionalSummaryEn = "Balanced conversation; maintaining friendly presence and natural rhythm.",
                    emotionalSummaryAr = "حوار متزن وطبيعي؛ حضور ودود وإيقاع صوتي متناسق."
                )
            }
        }
    }
}
