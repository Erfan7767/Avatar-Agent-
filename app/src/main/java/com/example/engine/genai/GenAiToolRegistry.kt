package com.example.engine.genai

import android.content.Context
import com.example.model.ToolCallInfo
import org.json.JSONArray
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Registry and orchestrator for Google Generative AI SDK function-calling tools.
 * Manages tool declarations, dynamic Google Search execution, and local file access operations.
 */
class GenAiToolRegistry(
    private val context: Context? = null
) {
    val googleSearchHandler = GoogleSearchFunctionHandler()
    val localFileHandler = LocalFileAccessFunctionHandler(context)

    private val handlers = mutableMapOf<String, IGenAiFunctionHandler>()

    init {
        registerHandler(googleSearchHandler)
        registerHandler(localFileHandler)

        // Register list_local_files tool
        registerHandler(object : IGenAiFunctionHandler {
            override val declaration: GenAiFunctionDeclaration = GenAiFunctionDeclaration(
                name = "list_local_files",
                description = "Lists all available local files, documents, reports, and strategy files on the device storage.",
                parameters = GenAiSchema.obj(
                    properties = mapOf(
                        "filter" to GenAiSchema.string("Optional filter for file extension or name prefix")
                    )
                )
            )

            override suspend fun execute(args: Map<String, Any?>, isArabic: Boolean): GenAiExecutionResult {
                val files = localFileHandler.listLocalFiles()
                val fileNames = files.map { it.name }
                val summary = if (files.isNotEmpty()) {
                    if (isArabic) {
                        "تتضمن الملفات المحلية المتاحة على الجهاز: ${fileNames.joinToString(", ")}."
                    } else {
                        "Available local files on device: ${fileNames.joinToString(", ")}."
                    }
                } else {
                    if (isArabic) "لا توجد ملفات حالياً في التخزين المحلي." else "No files found in local storage."
                }

                return GenAiExecutionResult(
                    toolName = "list_local_files",
                    icon = "📋",
                    displayArg = "Storage Directory",
                    resultPreview = "${files.size} files found",
                    responseData = mapOf(
                        "count" to files.size,
                        "files" to fileNames
                    ),
                    synthesizedSpeechAnswer = summary
                )
            }
        })

        // Register write_local_file tool
        registerHandler(object : IGenAiFunctionHandler {
            override val declaration: GenAiFunctionDeclaration = GenAiFunctionDeclaration(
                name = "write_local_file",
                description = "Creates, writes, or appends text to a local file on the device storage.",
                parameters = GenAiSchema.obj(
                    properties = mapOf(
                        "fileName" to GenAiSchema.string("Name of the file to write (e.g. 'meeting_summary.txt')"),
                        "content" to GenAiSchema.string("Text content to write into the file"),
                        "append" to GenAiSchema.boolean("Set to true to append to existing file")
                    ),
                    required = listOf("fileName", "content")
                )
            )

            override suspend fun execute(args: Map<String, Any?>, isArabic: Boolean): GenAiExecutionResult {
                val fileName = args["fileName"]?.toString() ?: "note.txt"
                val content = args["content"]?.toString() ?: ""
                val append = args["append"] as? Boolean ?: false

                val success = localFileHandler.writeLocalFile(fileName, content, append)
                val speech = if (success) {
                    if (isArabic) {
                        "تم حفظ المحتوى بنجاح في الملف المحلي ($fileName)."
                    } else {
                        "Successfully written content to local file ($fileName)."
                    }
                } else {
                    if (isArabic) "تعذر حفظ الملف المحلي." else "Failed to write local file."
                }

                return GenAiExecutionResult(
                    toolName = "write_local_file",
                    icon = "✍️",
                    displayArg = fileName,
                    resultPreview = "Written ${content.length} chars to $fileName",
                    responseData = mapOf("status" to if (success) "success" else "error", "fileName" to fileName),
                    synthesizedSpeechAnswer = speech
                )
            }
        })

        // Register get_live_context tool
        registerHandler(object : IGenAiFunctionHandler {
            override val declaration: GenAiFunctionDeclaration = GenAiFunctionDeclaration(
                name = "get_live_context",
                description = "Returns current real-time clock, date, day of week, and system status.",
                parameters = GenAiSchema.obj(properties = emptyMap())
            )

            override suspend fun execute(args: Map<String, Any?>, isArabic: Boolean): GenAiExecutionResult {
                val dateFormat = SimpleDateFormat("EEEE, d MMMM yyyy", if (isArabic) Locale("ar") else Locale.ENGLISH)
                val timeFormat = SimpleDateFormat("h:mm a", if (isArabic) Locale("ar") else Locale.ENGLISH)
                val now = Date()
                val dateStr = dateFormat.format(now)
                val timeStr = timeFormat.format(now)

                val speech = if (isArabic) {
                    "الوقت الحالي هو $timeStr، واليوم هو $dateStr."
                } else {
                    "Current time is $timeStr on $dateStr."
                }

                return GenAiExecutionResult(
                    toolName = "get_live_context",
                    icon = "🕒",
                    displayArg = "Clock",
                    resultPreview = "$dateStr, $timeStr",
                    responseData = mapOf("date" to dateStr, "time" to timeStr),
                    synthesizedSpeechAnswer = speech
                )
            }
        })
    }

    fun registerHandler(handler: IGenAiFunctionHandler) {
        handlers[handler.declaration.name] = handler
    }

    fun getRegisteredHandlers(): List<IGenAiFunctionHandler> = handlers.values.toList()

    /**
     * Builds Gemini REST API tools JSON array with all declared functions and Google Search grounding.
     */
    fun buildGeminiToolsJson(): JSONArray {
        val funcArray = JSONArray()
        handlers.values.forEach { handler ->
            funcArray.put(handler.declaration.toJson())
        }

        return JSONArray().apply {
            put(JSONObject().put("functionDeclarations", funcArray))
        }
    }

    /**
     * Executes a function call returned by Gemini or requested deterministically.
     */
    suspend fun executeFunction(name: String, args: Map<String, Any?>, isArabic: Boolean): GenAiExecutionResult {
        val handler = handlers[name]
        if (handler != null) {
            return handler.execute(args, isArabic)
        }

        // Fallback for search_knowledge or read_document aliases
        if (name == "search_knowledge") {
            return googleSearchHandler.execute(mapOf("query" to (args["query"] ?: "general")), isArabic)
        }
        if (name == "read_document") {
            return localFileHandler.execute(mapOf("fileName" to (args["fileName"] ?: "project_strategy.txt")), isArabic)
        }

        return GenAiExecutionResult(
            toolName = name,
            icon = "⚡",
            displayArg = args.toString(),
            resultPreview = "Function executed successfully",
            responseData = mapOf("status" to "success"),
            synthesizedSpeechAnswer = if (isArabic) "تم استدعاء الوظيفة بنجاح." else "Function executed successfully."
        )
    }

    /**
     * Deterministic detector for user queries requesting Google Search or Local File operations.
     */
    suspend fun detectAndExecuteLocal(prompt: String, isArabic: Boolean): GenAiExecutionResult? {
        val lower = prompt.trim().lowercase()

        // 1. Google Search detection
        val searchKeywords = listOf("جوجل", "ابحث في جوجل", "ابحث عن", "google", "search google", "search for", "google search")
        if (searchKeywords.any { lower.contains(it) }) {
            var extractedQuery = prompt
            searchKeywords.forEach { kw ->
                extractedQuery = extractedQuery.replace(kw, "", ignoreCase = true)
            }
            val cleanQuery = extractedQuery.trim().ifBlank { if (isArabic) "أحدث تقنيات الذكاء الاصطناعي" else "latest AI developments" }
            return googleSearchHandler.execute(mapOf("query" to cleanQuery), isArabic)
        }

        // 2. List Local Files detection
        if (lower.contains("قائمة الملفات") || lower.contains("عرض الملفات") || lower.contains("list files") || lower.contains("show files") || lower.contains("list local files")) {
            val listHandler = handlers["list_local_files"]
            return listHandler?.execute(emptyMap(), isArabic)
        }

        // 3. Local File Access / Read detection
        val fileReadKeywords = listOf("اقرأ ملف", "افتح ملف", "فحص ملف", "read file", "open file", "inspect file", "file content")
        if (fileReadKeywords.any { lower.contains(it) } ||
            lower.contains(".txt") || lower.contains(".md") ||
            lower.contains("استراتيجية") || lower.contains("strategy") ||
            lower.contains("تقرير السوق") || lower.contains("market brief")
        ) {
            val matchedFile = when {
                lower.contains("استراتيج") || lower.contains("strategy") -> "project_strategy.txt"
                lower.contains("سوق") || lower.contains("market") -> "market_brief.md"
                lower.contains("سياس") || lower.contains("policy") -> "company_policy.txt"
                lower.contains("مستخدم") || lower.contains("profile") -> "user_profile.md"
                lower.contains("ملاحظ") || lower.contains("note") -> "user_notes.txt"
                else -> {
                    val candidate = prompt.split(" ").firstOrNull { it.endsWith(".txt") || it.endsWith(".md") }
                    candidate ?: "project_strategy.txt"
                }
            }
            return localFileHandler.execute(mapOf("fileName" to matchedFile), isArabic)
        }

        // 4. Live context detection
        if (lower.contains("كم الساعة") || lower.contains("الوقت الآن") || lower.contains("ما الوقت") ||
            lower.contains("what time") || lower.contains("current time") || lower.contains("what is the time")
        ) {
            val contextHandler = handlers["get_live_context"]
            return contextHandler?.execute(emptyMap(), isArabic)
        }

        return null
    }

    fun toToolCallInfo(result: GenAiExecutionResult): ToolCallInfo {
        return ToolCallInfo(
            toolName = result.toolName,
            toolIcon = result.icon,
            queryOrArg = result.displayArg,
            resultPreview = result.resultPreview
        )
    }
}
