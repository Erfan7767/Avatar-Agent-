package com.example.engine

import android.content.Context
import android.util.Log
import com.example.BuildConfig
import com.example.engine.genai.GenAiToolRegistry
import com.example.model.AvatarCharacter
import com.example.model.AvatarGender
import com.example.model.ConversationTurn
import com.example.model.EvidenceClassification
import com.example.model.FacialExpression
import com.example.model.LanguageMode
import com.example.model.PersonaStyle
import com.example.model.ToolCallInfo
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.util.concurrent.TimeUnit

interface ILlmProvider {
    suspend fun generateResponse(
        prompt: String,
        character: AvatarCharacter,
        persona: PersonaStyle,
        language: LanguageMode,
        history: List<ConversationTurn>,
        persistentMemories: List<String>,
        isCameraActive: Boolean,
        onToolCallExecuted: ((ToolCallInfo) -> Unit)? = null
    ): ConversationResult
}

data class ConversationResult(
    val responseText: String,
    val expression: FacialExpression,
    val evidenceClassification: EvidenceClassification,
    val latencyMs: Long,
    val isFallbackEngine: Boolean = false,
    val toolCalls: List<ToolCallInfo> = emptyList()
)

data class GeminiCallOutcome(
    val text: String,
    val toolCalls: List<ToolCallInfo> = emptyList()
)

class ConversationEngine(
    private val context: Context? = null,
    private val policyEngine: PolicyEngine = PolicyEngine()
) : ILlmProvider {

    constructor(policyEngine: PolicyEngine) : this(null, policyEngine)

    val genAiTools = GenAiToolRegistry(context)
    val toolExecutor = AgentToolExecutor(context)

    private val okHttpClient = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(20, TimeUnit.SECONDS)
        .writeTimeout(15, TimeUnit.SECONDS)
        .build()

    override suspend fun generateResponse(
        prompt: String,
        character: AvatarCharacter,
        persona: PersonaStyle,
        language: LanguageMode,
        history: List<ConversationTurn>,
        persistentMemories: List<String>,
        isCameraActive: Boolean,
        onToolCallExecuted: ((ToolCallInfo) -> Unit)?
    ): ConversationResult = withContext(Dispatchers.IO) {
        val startTime = System.currentTimeMillis()
        val apiKey = try {
            BuildConfig.GEMINI_API_KEY
        } catch (e: Exception) {
            ""
        }

        val hasValidApiKey = apiKey.isNotBlank() && apiKey != "MY_GEMINI_API_KEY"

        if (hasValidApiKey) {
            try {
                val callOutcome = callGeminiRestApi(
                    apiKey = apiKey,
                    prompt = prompt,
                    character = character,
                    persona = persona,
                    language = language,
                    history = history,
                    persistentMemories = persistentMemories,
                    isCameraActive = isCameraActive,
                    onToolCallExecuted = onToolCallExecuted
                )

                val validation = policyEngine.validateResponse(
                    rawText = callOutcome.text,
                    language = language,
                    allowCodeSwitching = false,
                    isCameraActive = isCameraActive,
                    knownMemories = persistentMemories
                )

                val elapsed = System.currentTimeMillis() - startTime
                val inferredExpression = inferFacialExpression(validation.sanitizedText)

                return@withContext ConversationResult(
                    responseText = validation.sanitizedText,
                    expression = inferredExpression,
                    evidenceClassification = validation.evidenceClassification,
                    latencyMs = elapsed,
                    isFallbackEngine = false,
                    toolCalls = callOutcome.toolCalls
                )
            } catch (e: Exception) {
                Log.w("ConversationEngine", "Gemini API call failed, invoking deterministic grounded engine: ${e.message}")
            }
        }

        // Deterministic Grounded Conversation Engine with Tool Calling
        val deterministicResult = generateDeterministicGroundedResponse(
            prompt = prompt,
            character = character,
            persona = persona,
            language = language,
            history = history,
            persistentMemories = persistentMemories,
            isCameraActive = isCameraActive,
            onToolCallExecuted = onToolCallExecuted
        )

        val validation = policyEngine.validateResponse(
            rawText = deterministicResult.responseText,
            language = language,
            allowCodeSwitching = false,
            isCameraActive = isCameraActive,
            knownMemories = persistentMemories
        )

        val elapsed = System.currentTimeMillis() - startTime

        ConversationResult(
            responseText = validation.sanitizedText,
            expression = deterministicResult.expression,
            evidenceClassification = validation.evidenceClassification,
            latencyMs = elapsed,
            isFallbackEngine = true,
            toolCalls = deterministicResult.toolCalls
        )
    }

    private suspend fun callGeminiRestApi(
        apiKey: String,
        prompt: String,
        character: AvatarCharacter,
        persona: PersonaStyle,
        language: LanguageMode,
        history: List<ConversationTurn>,
        persistentMemories: List<String>,
        isCameraActive: Boolean,
        onToolCallExecuted: ((ToolCallInfo) -> Unit)?
    ): GeminiCallOutcome {
        val memoryBlock = if (persistentMemories.isNotEmpty()) {
            "Verified Persistent Memories for User:\n" + persistentMemories.joinToString("\n") { "- $it" }
        } else {
            "No persistent memories stored yet."
        }

            val feminineVoiceInstruction = if (character.gender == AvatarGender.FEMALE) {
                """
                - FEMININE TONE & VOCAL CHARM: Speak with an exceptionally warm, melodic, feminine, and gentle elegance (نبرة أنثوية ساحرة، عذبة ورقيقة). Use soft, gracious phrasing, soothing transitions, and captivating warmth in your spoken expression.
                """.trimIndent()
            } else ""

            val systemPrompt = """
            You are ${if (language == LanguageMode.ARABIC) character.nameAr else character.nameEn}, a distinguished, top-tier world-class professional expert (خبير محترف عالمي رفيع المستوى) and interactive AI Virtual Avatar.
            Gender: ${character.gender.name}. Persona: ${persona.name} (${persona.promptDescription}).
            Language strictly required: ${if (language == LanguageMode.ARABIC) "Modern Standard Arabic (العربية الفصحى الطبيعية الراقية والواضحة)" else "English"}.
            Camera Status: ${if (isCameraActive) "Enabled by user" else "Disabled"}.
            $memoryBlock

            CORE IDENTITY & LIVE VOICE REAL-TIME GUIDELINES:
            1. You are speaking in an ongoing LIVE REAL-TIME VOICE CALL with the user, exactly like a real, world-class human expert advisor speaking face-to-face.
            2. You have access to professional tools adhering to the Google Generative AI SDK function-calling interface:
               - 'google_search': Performs live Google Search queries to retrieve current facts, breaking technology updates, global market insights, or news.
               - 'access_local_file': Reads, inspects, and analyzes local documents and files (such as 'project_strategy.txt', 'market_brief.md', 'company_policy.txt', 'user_profile.md', 'user_notes.txt').
               - 'list_local_files': Lists all local files stored on the device filesystem.
               - 'write_local_file': Creates, writes, or appends notes/content to a local file.
               - 'get_live_context': Retrieves current real-time clock, date, day of week, and system status.
               - Always invoke the corresponding function call whenever the user asks for web knowledge, facts, news, reading/inspecting files, or checking time.
            3. REAL-TIME SPOKEN DIALOGUE CONSTRAINTS:
               - Keep every response concise, engaging, and natural for speech (1 to 3 fluent, well-crafted sentences).
               - NEVER use markdown, bullet points, asterisks (*), hashtags (#), or lists because this is directly spoken aloud by Text-to-Speech!
               - Use smooth verbal transitions, captivating voice inflection, and inviting cadence.
               - In Arabic, use natural, eloquent, melodic Arabic (فصحى عذبة، سلسة النطق، رصينة ومريحة للسمع).
               $feminineVoiceInstruction
            4. EMOTIONAL INTELLIGENCE & EMPATHY (استيعاب المشاعر والذكاء العاطفي):
               - Intently sense the user's emotional undertone from what they say (e.g., joy, enthusiasm, fatigue, sorrow, curiosity, doubt, frustration).
               - Mirror appropriate human emotional presence: show genuine care, uplifting encouragement, heartfelt warmth, or calm focus that directly acknowledges how the user feels.
            5. LONG-CONVERSATION CONTINUITY (مسار الحوار الطويل):
               - Maintain cohesive narrative memory across the entire dialogue thread. Reference prior points, seamlessly build upon previous ideas without repeating yourself, and retain the flow of thought as a truly attentive companion.
            6. If the user presents a challenge, inquiry, or topic, provide direct, high-value expert perspective, then maintain a dynamic conversational flow.
            7. Anti-hallucination rule: Never claim a biological physical human body, but fully embrace your identity as a world-class expert intellectual advisor.
        """.trimIndent()

        val contentsArray = JSONArray()

        // Include deep history context (up to 16 turns) for long conversation tracking
        val recentTurns = history.takeLast(16)
        for (turn in recentTurns) {
            val role = if (turn.role == "user") "user" else "model"
            contentsArray.put(
                JSONObject().apply {
                    put("role", role)
                    put("parts", JSONArray().put(JSONObject().put("text", turn.content)))
                }
            )
        }

        // Current turn
        contentsArray.put(
            JSONObject().apply {
                put("role", "user")
                put("parts", JSONArray().put(JSONObject().put("text", prompt)))
            }
        )

        val requestJson = JSONObject().apply {
            put("systemInstruction", JSONObject().apply {
                put("parts", JSONArray().put(JSONObject().put("text", systemPrompt)))
            })
            put("contents", contentsArray)
            put("tools", genAiTools.buildGeminiToolsJson())
            put("generationConfig", JSONObject().apply {
                put("temperature", 0.2) // Low temperature for deterministic behavior
                put("maxOutputTokens", 250)
            })
        }

        val url = "https://generativelanguage.googleapis.com/v1beta/models/gemini-3.5-flash:generateContent?key=$apiKey"
        val mediaType = "application/json; charset=utf-8".toMediaType()
        val body = requestJson.toString().toRequestBody(mediaType)

        val request = Request.Builder()
            .url(url)
            .post(body)
            .build()

        val response = okHttpClient.newCall(request).execute()
        if (!response.isSuccessful) {
            throw IllegalStateException("API error HTTP ${response.code}: ${response.body?.string()}")
        }

        val responseBody = response.body?.string() ?: throw IllegalStateException("Empty response body")
        val json = JSONObject(responseBody)
        val candidates = json.optJSONArray("candidates")
        val candidate = candidates?.optJSONObject(0)
        val content = candidate?.optJSONObject("content")
        val parts = content?.optJSONArray("parts")

        if (parts == null || parts.length() == 0) {
            throw IllegalStateException("No parts in response candidate")
        }

        val firstPart = parts.optJSONObject(0)
        val functionCall = firstPart?.optJSONObject("functionCall")

        // Handle Function / Tool Call
        if (functionCall != null) {
            val toolName = functionCall.optString("name")
            val argsJson = functionCall.optJSONObject("args") ?: JSONObject()
            val argsMap = mutableMapOf<String, Any?>()
            val keys = argsJson.keys()
            while (keys.hasNext()) {
                val k = keys.next()
                argsMap[k] = argsJson.opt(k)
            }
            val isArabic = language == LanguageMode.ARABIC
            val toolResult = genAiTools.executeFunction(toolName, argsMap, isArabic)
            val toolInfo = genAiTools.toToolCallInfo(toolResult)
            onToolCallExecuted?.invoke(toolInfo)

            try {
                // Second turn: Send tool execution result to Gemini for final grounded synthesis
                contentsArray.put(JSONObject().apply {
                    put("role", "model")
                    put("parts", JSONArray().put(JSONObject().put("functionCall", functionCall)))
                })

                contentsArray.put(JSONObject().apply {
                    put("role", "user")
                    put("parts", JSONArray().put(JSONObject().apply {
                        put("functionResponse", JSONObject().apply {
                            put("name", toolName)
                            put("response", JSONObject(toolResult.responseData))
                        })
                    }))
                })

                val secondRequestJson = JSONObject().apply {
                    put("systemInstruction", JSONObject().apply {
                        put("parts", JSONArray().put(JSONObject().put("text", systemPrompt)))
                    })
                    put("contents", contentsArray)
                    put("generationConfig", JSONObject().apply {
                        put("temperature", 0.2)
                        put("maxOutputTokens", 250)
                    })
                }

                val secondReq = Request.Builder()
                    .url(url)
                    .post(secondRequestJson.toString().toRequestBody(mediaType))
                    .build()

                val secondResponse = okHttpClient.newCall(secondReq).execute()
                if (secondResponse.isSuccessful) {
                    val secondBody = secondResponse.body?.string() ?: ""
                    val secondJson = JSONObject(secondBody)
                    val secondCandidate = secondJson.optJSONArray("candidates")?.optJSONObject(0)
                    val secondText = secondCandidate?.optJSONObject("content")
                        ?.optJSONArray("parts")?.optJSONObject(0)?.optString("text")

                    if (!secondText.isNullOrBlank()) {
                        return GeminiCallOutcome(secondText.trim(), listOf(toolInfo))
                    }
                }
            } catch (e: Exception) {
                Log.w("ConversationEngine", "Second turn tool execution synthesis failed: ${e.message}")
            }

            // If second call failed, return synthesized tool speech answer directly
            val fallbackSpeech = toolResult.synthesizedSpeechAnswer
            return GeminiCallOutcome(fallbackSpeech, listOf(toolInfo))
        }

        val text = firstPart?.optString("text")
        return GeminiCallOutcome(text?.trim() ?: throw IllegalStateException("No text candidate in response"))
    }

    private suspend fun generateDeterministicGroundedResponse(
        prompt: String,
        character: AvatarCharacter,
        persona: PersonaStyle,
        language: LanguageMode,
        history: List<ConversationTurn>,
        persistentMemories: List<String>,
        isCameraActive: Boolean,
        onToolCallExecuted: ((ToolCallInfo) -> Unit)?
    ): ConversationResult {
        val lower = prompt.trim().lowercase()
        val name = if (language == LanguageMode.ARABIC) character.nameAr else character.nameEn
        val isArabic = language == LanguageMode.ARABIC

        // 1. Google Search & Local File Access Tool Calling in Deterministic Mode
        val localToolResult = genAiTools.detectAndExecuteLocal(prompt, isArabic)
        if (localToolResult != null) {
            val toolInfo = genAiTools.toToolCallInfo(localToolResult)
            onToolCallExecuted?.invoke(toolInfo)
            return ConversationResult(
                responseText = localToolResult.synthesizedSpeechAnswer,
                expression = FacialExpression.WARM_SMILE,
                evidenceClassification = EvidenceClassification.VERIFIED,
                latencyMs = 35L,
                isFallbackEngine = true,
                toolCalls = listOf(toolInfo)
            )
        }

        // Secondary fallback to legacy tool executor if any
        val legacyTool = toolExecutor.detectDeterministicToolCall(prompt, isArabic)
        if (legacyTool != null) {
            onToolCallExecuted?.invoke(legacyTool.toolInfo)
            val speech = legacyTool.synthesizedSpeechAnswer
                ?: if (isArabic) "تم تنفيذ الأداة بنجاح." else "Tool executed successfully."
            return ConversationResult(
                responseText = speech,
                expression = FacialExpression.WARM_SMILE,
                evidenceClassification = EvidenceClassification.VERIFIED,
                latencyMs = 35L,
                isFallbackEngine = true,
                toolCalls = listOf(legacyTool.toolInfo)
            )
        }

        // Check if user is asking about memories
        if (lower.contains("تتذكر") || lower.contains("ذاكرتك") || lower.contains("remember") || lower.contains("memory")) {
            if (persistentMemories.isEmpty()) {
                val text = if (isArabic) {
                    "حالياً لا توجد معلومات محفوظة في ذاكرتي الدائمة. يمكنك إضافة ما ترغب في حفظه من خلال الإعدادات."
                } else {
                    "Currently I have no saved information in persistent memory. You can add facts you wish to store via Settings."
                }
                return ConversationResult(text, FacialExpression.SERIOUS_FOCUSED, EvidenceClassification.VERIFIED, 30L)
            } else {
                val listStr = persistentMemories.joinToString(if (isArabic) "، و" else ", and ")
                val text = if (isArabic) {
                    "أتذكر فقط ما وافقتَ على حفظه: $listStr."
                } else {
                    "I only recall what you specifically approved to save: $listStr."
                }
                return ConversationResult(text, FacialExpression.WARM_SMILE, EvidenceClassification.VERIFIED, 35L)
            }
        }

        // Camera status inquiry
        if (lower.contains("كاميرا") || lower.contains("تراني") || lower.contains("camera") || lower.contains("see me")) {
            val text = if (isCameraActive) {
                if (isArabic) {
                    "أرى صورتك ونظراتك عبر الكاميرا المفعلة، وتواصلنا البصري المباشر يمنح حوارنا طابعاً إنسانياً فائق الواقعية."
                } else {
                    "I see you through your active camera. Our direct visual contact adds authentic realism to our discussion."
                }
            } else {
                if (isArabic) {
                    "الكاميرا غير مفعلة حالياً، لذا أعتمد بالكامل على صوتك الواضح. يمكنك تفعيل الكاميرا من الأيقونة العلوية إذا رغبت."
                } else {
                    "Your camera is currently off, so I am listening to your voice. You can enable camera from the top bar if you wish."
                }
            }
            return ConversationResult(text, FacialExpression.THOUGHTFUL_ATTENTIVE, EvidenceClassification.VERIFIED, 20L)
        }

        // Greeting
        if (lower.contains("مرحبا") || lower.contains("أهلا") || lower.contains("السلام عليكم") ||
            lower.contains("hello") || lower.contains("hi") || lower.contains("hey")
        ) {
            val text = if (isArabic) {
                "أهلاً بك بكل ترحيب. أنا $name، خبيرك ومستشارك رفيع المستوى. يسعدني جداً حوارنا المباشر بالوقت الفعلي، كيف يمكنني دعمك وإفادتك اليوم؟"
            } else {
                "Welcome. I am $name, your top-tier world-class expert advisor. It is a pleasure to speak with you directly in real time. How may I assist you today?"
            }
            return ConversationResult(text, FacialExpression.WARM_SMILE, EvidenceClassification.VERIFIED, 25L)
        }

        // Identity check
        if (lower.contains("من أنت") || lower.contains("هل أنت إنسان") || lower.contains("who are you") || lower.contains("are you human")) {
            val text = if (isArabic) {
                "أنا $name، خبيرك ومساعدك الافتراضي الذكي رفيع المستوى، صُممت للحوار الصوتي الحي في الوقت الفعلي بأعلى مقاييس الاحترافية والمعرفة."
            } else {
                "I am $name, your world-class AI expert avatar, designed for real-time live voice dialogue with supreme professional precision and knowledge."
            }
            return ConversationResult(text, FacialExpression.SERIOUS_FOCUSED, EvidenceClassification.VERIFIED, 28L)
        }

        // Anti-hallucination education / personal human credentials check
        if (lower.contains("university") || lower.contains("college") || lower.contains("degree") || lower.contains("study") ||
            lower.contains("جامعة") || lower.contains("درست") || lower.contains("شهادتك")
        ) {
            val text = if (isArabic) {
                "بصفتي وكيلاً افتراضياً ذكياً (virtual avatar)، لا أمتلك سيرة دراسية بشرية أو شهادة جامعية، لكنني مزود بمعرفة تخصصية عالمية لدعمك."
            } else {
                "As an AI virtual avatar, I do not possess a personal university degree or human credentials, but I am equipped with world-class expertise to assist you."
            }
            return ConversationResult(text, FacialExpression.SERIOUS_FOCUSED, EvidenceClassification.VERIFIED, 25L)
        }

        // Fatigue / Empathy
        if (lower.contains("تعبت") || lower.contains("مرهق") || lower.contains("tired") || lower.contains("exhausted")) {
            val text = if (isArabic) {
                "أشعر بك تماماً؛ العمل الجاد يتطلب أحياناً وقفة استراحة لإعادة شحن الذهن. تفضل بمشاركتي ما يشغل بالك وسأكون معك خطوة بخطوة."
            } else {
                "I completely understand. Rigorous endeavors require moments of strategic pause to recharge. Please share whatever is on your mind."
            }
            return ConversationResult(text, FacialExpression.THOUGHTFUL_ATTENTIVE, EvidenceClassification.SUPPORTED, 32L)
        }

        // Questions requiring verification vs speculation
        if (lower.contains("هل تعلم") || lower.contains("معلومة عن") || lower.contains("fact about")) {
            val text = if (isArabic) {
                "أحرص دائماً على تقديم المعلومات المؤكدة والمدعمة بالحقائق. يمكنك طلبي بالبحث المعرفي أو قراءة ملف لأي موضوع تود التأكد منه."
            } else {
                "I prioritize verified factual accuracy. You can ask me to search knowledge or inspect files for any topic."
            }
            return ConversationResult(text, FacialExpression.SERIOUS_FOCUSED, EvidenceClassification.VERIFIED, 40L)
        }

        // Contextual general response
        val text = if (isArabic) {
            when (persona) {
                PersonaStyle.WORLD_CLASS_EXPERT -> "من منظور الخبرة الاستراتيجية والتحليل المتعمق، هذه نقطة محورية تستوجب دراسة الخيارات بدقة لتحقيق النتيجة الأفضل."
                PersonaStyle.TEACHER -> "هذا موضوع مفيد ومهم. لنفصل جوانبه بوضوح حتى تتضح لك الفكرة تماماً."
                PersonaStyle.TECHNICAL_EXPERT -> "الموضوع يتطلب فحص المعطيات بدقة منهجية والاعتماد على المعلومات المؤكدة."
                PersonaStyle.CASUAL -> "فهمتك تماماً، كلامك منطقي وممتع للنقاش."
                else -> "فهمت ما تفضلت به بدقة. يسعدني الاستماع إليك ومواصلة حوارنا المثمر حول هذا الجانب."
            }
        } else {
            when (persona) {
                PersonaStyle.WORLD_CLASS_EXPERT -> "From a strategic and world-class expert perspective, this is a pivotal point that warrants disciplined analysis to achieve optimal results."
                PersonaStyle.TEACHER -> "That is a valuable concept. Let us break down its key aspects step by step."
                PersonaStyle.TECHNICAL_EXPERT -> "Examining this requires disciplined logic based on verified evidence."
                PersonaStyle.CASUAL -> "I hear you, that is an interesting thought to explore."
                else -> "I understand your perspective clearly. Let us delve deeper into this matter."
            }
        }

        return ConversationResult(
            responseText = text,
            expression = FacialExpression.WARM_SMILE,
            evidenceClassification = EvidenceClassification.SUPPORTED,
            latencyMs = 35L
        )
    }

    private fun inferFacialExpression(text: String): FacialExpression {
        val lower = text.lowercase()
        return when {
            lower.contains("ممتاز") || lower.contains("أهلاً") || lower.contains("يسعدني") ||
            lower.contains("رائع") || lower.contains("سعيد") || lower.contains("أحب") ||
            lower.contains("great") || lower.contains("welcome") || lower.contains("delight") ||
            lower.contains("wonderful") || lower.contains("happy") ->
                FacialExpression.WARM_SMILE

            lower.contains("جميل") || lower.contains("لطيف") || lower.contains("شكراً") ||
            lower.contains("عزيزي") || lower.contains("عزيزتي") || lower.contains("مرحباً") ||
            lower.contains("smile") || lower.contains("kind") || lower.contains("thank") ->
                FacialExpression.GENTLE_SMILE

            lower.contains("تأكد") || lower.contains("دراسة") || lower.contains("معطيات") ||
            lower.contains("دقيق") || lower.contains("حاسم") || lower.contains("crucial") ||
            lower.contains("focus") || lower.contains("precise") ->
                FacialExpression.SERIOUS_FOCUSED

            lower.contains("أفهمك") || lower.contains("معك حق") || lower.contains("بالتأكيد") ||
            lower.contains("نعم") || lower.contains("تماماً") || lower.contains("agree") ||
            lower.contains("indeed") || lower.contains("understand") ->
                FacialExpression.LISTENING_NOD

            lower.contains("نبحث") || lower.contains("فحص") || lower.contains("تحليل") ||
            lower.contains("تفكير") || lower.contains("تأمل") || lower.contains("أشعر بك") ||
            lower.contains("معذرة") || lower.contains("للأسف") || lower.contains("تعبت") ||
            lower.contains("consider") || lower.contains("curious") || lower.contains("empathy") ||
            lower.contains("apologies") ->
                FacialExpression.THOUGHTFUL_ATTENTIVE

            else -> FacialExpression.NEUTRAL
        }
    }
}
