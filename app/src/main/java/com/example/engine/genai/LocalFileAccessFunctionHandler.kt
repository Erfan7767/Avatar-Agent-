package com.example.engine.genai

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Local File Access dynamic operation handlers implementing the Google Generative AI SDK function-calling interface.
 */
class LocalFileAccessFunctionHandler(
    private val context: Context? = null
) : IGenAiFunctionHandler {

    private val builtInTemplates = mapOf(
        "project_strategy.txt" to """
            [وثيقة استراتيجية المشروع - Project Strategy 2026]
            - الهدف: تقديم وكيل محادثة صوتية تفاعلي فوري بمستوى خبير عالمي.
            - البنية التقنية: خط أنابيب منخفض الكمون (Ultra-Low Latency Pipeline) يجمع بين التعرف الصوتي (ASR)، واستدعاء الأدوات الذكي (Tool Calling)، وتوليف الكلام (TTS) وتزامن الشفاه (Viseme Lip-Sync).
            - محاور التميز: مقاطعة ذكية فورية (Barge-In)، تبادل أدوار تلقائي حر، وحوكمة معرفية صارمة تمنع الهلوسة.
            - الخصوصية: معالجة وتخزين محلي للبيانات الحساسة على جهاز المستخدم.
        """.trimIndent(),
        "market_brief.md" to """
            [تقرير اتجاهات السوق - Market Brief 2026]
            - نمو هائل في الاعتماد على واجهات الوكلاء الأذكياء متعددي الوسائط (Multimodal AI Agents).
            - أهم ميزة تنافسية هي استدعاء الأدوات التلقائي (Tool Calling) للوصول للحقائق والملفات في الوقت الفعلي.
            - المستخدمون يفضلون الحوار الطبيعي المباشر المستمر بدون الحاجة للنقر اليدوي المتكرر.
        """.trimIndent(),
        "company_policy.txt" to """
            [وثيقة الأمان والسياسات المؤسسية - Governance & Safety]
            - الشفافية: الوكيل يعرف عن نفسه دائماً كخبير افتراضي ذكي ولا يدعي هوية إنسانية بيولوجية.
            - موثوقية المعلومات: التحقق من المصادر واستدعاء أدوات المعرفة والملفات قبل تقديم الاستشارات التخصصية.
            - الحفاظ التام على أمان بيانات المستخدم وعدم مشاركتها مع أطراف ثالثة.
        """.trimIndent(),
        "user_profile.md" to """
            [ملف تفضيلات المستخدم - User Profile]
            - النمط: مكالمة صوتية مباشرة في الوقت الفعلي (Real-Time Voice Call).
            - مستوى الخبير: خبير محترف عالمي رفيع المستوى (World-Class Expert).
            - الأدوات: تفعيل البحث في جوجل والوصول للملفات المحلية.
            - اللغة المفضلة: العربية الفصحى الراقية والإنجليزية المتقنة.
        """.trimIndent()
    )

    init {
        // Ensure standard files exist locally in application internal storage
        try {
            context?.let { ctx ->
                builtInTemplates.forEach { (name, content) ->
                    val f = File(ctx.filesDir, name)
                    if (!f.exists()) {
                        f.writeText(content)
                    }
                }
            }
        } catch (_: Exception) {}
    }

    override val declaration: GenAiFunctionDeclaration = GenAiFunctionDeclaration(
        name = "access_local_file",
        description = "Reads, inspects, or searches local documents, configuration files, and strategy notes on the device storage.",
        parameters = GenAiSchema.obj(
            properties = mapOf(
                "fileName" to GenAiSchema.string("Name of the local file (e.g. 'project_strategy.txt', 'market_brief.md', 'company_policy.txt', 'user_profile.md', or 'user_notes.txt')"),
                "operation" to GenAiSchema.string("Operation to perform: 'read' (default), 'list', or 'search'")
            ),
            required = listOf("fileName")
        )
    )

    override suspend fun execute(args: Map<String, Any?>, isArabic: Boolean): GenAiExecutionResult = withContext(Dispatchers.IO) {
        val rawFileName = args["fileName"]?.toString() ?: "project_strategy.txt"
        val operation = args["operation"]?.toString() ?: "read"

        val sanitizedName = rawFileName.trim().replace(Regex("[^a-zA-Z0-9._-]"), "")
        val targetFile = context?.let { File(it.filesDir, sanitizedName) }

        var content: String? = null
        if (targetFile != null && targetFile.exists() && targetFile.isFile) {
            content = try { targetFile.readText() } catch (_: Exception) { null }
        }

        if (content == null) {
            val matchedTemplate = builtInTemplates.entries.firstOrNull {
                it.key.contains(sanitizedName, ignoreCase = true) || sanitizedName.contains(it.key.substringBefore("."), ignoreCase = true)
            }
            content = matchedTemplate?.value
        }

        val actualContent = content ?: if (isArabic) {
            "الملف \"$sanitizedName\" غير متوفر حالياً في وحدة التخزين المحلية. الملفات المتاحة تشمل: ${getAvailableFileNames().joinToString(", ")}."
        } else {
            "File \"$sanitizedName\" is not currently found in local storage. Available files: ${getAvailableFileNames().joinToString(", ")}."
        }

        val speech = if (isArabic) {
            "قمتُ بالوصول إلى الملف المحلي ($sanitizedName)؛ وخلاصته: ${actualContent.take(160).replace("\n", " ")}."
        } else {
            "Accessed local file ($sanitizedName). Executive summary: ${actualContent.take(160).replace("\n", " ")}."
        }

        val responseData = mapOf(
            "status" to if (content != null) "success" else "not_found",
            "fileName" to sanitizedName,
            "operation" to operation,
            "sizeBytes" to (targetFile?.length() ?: actualContent.length.toLong()),
            "content" to actualContent
        )

        GenAiExecutionResult(
            toolName = "access_local_file",
            icon = "📁",
            displayArg = sanitizedName,
            resultPreview = actualContent.take(80) + "...",
            responseData = responseData,
            synthesizedSpeechAnswer = speech
        )
    }

    fun listLocalFiles(): List<LocalFileInfo> {
        val list = mutableListOf<LocalFileInfo>()
        context?.let { ctx ->
            ctx.filesDir.listFiles()?.filter { it.isFile }?.forEach { f ->
                list.add(
                    LocalFileInfo(
                        name = f.name,
                        sizeBytes = f.length(),
                        lastModified = f.lastModified(),
                        formattedDate = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault()).format(Date(f.lastModified()))
                    )
                )
            }
        }
        if (list.isEmpty()) {
            builtInTemplates.forEach { (k, v) ->
                list.add(
                    LocalFileInfo(
                        name = k,
                        sizeBytes = v.length.toLong(),
                        lastModified = System.currentTimeMillis(),
                        formattedDate = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault()).format(Date())
                    )
                )
            }
        }
        return list
    }

    fun writeLocalFile(fileName: String, content: String, append: Boolean = false): Boolean {
        val sanitized = fileName.trim().replace(Regex("[^a-zA-Z0-9._-]"), "")
        if (sanitized.isBlank()) return false
        return try {
            context?.let { ctx ->
                val f = File(ctx.filesDir, sanitized)
                if (append) f.appendText(content) else f.writeText(content)
                true
            } ?: false
        } catch (_: Exception) {
            false
        }
    }

    private fun getAvailableFileNames(): List<String> {
        val diskFiles = context?.filesDir?.listFiles()?.map { it.name } ?: emptyList()
        return (diskFiles + builtInTemplates.keys).distinct()
    }

    data class LocalFileInfo(
        val name: String,
        val sizeBytes: Long,
        val lastModified: Long,
        val formattedDate: String
    )
}
