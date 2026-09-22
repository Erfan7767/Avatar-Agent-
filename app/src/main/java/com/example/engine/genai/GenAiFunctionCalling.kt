package com.example.engine.genai

import org.json.JSONArray
import org.json.JSONObject

/**
 * Google Generative AI SDK function-calling type system and specifications.
 * Aligns with the official Google Gen AI / Gemini Function Calling specification:
 * - Tools & FunctionDeclarations
 * - Schema (STRING, INTEGER, NUMBER, BOOLEAN, ARRAY, OBJECT)
 * - FunctionCall & FunctionResponse
 */

enum class GenAiType(val jsonString: String) {
    STRING("STRING"),
    INTEGER("INTEGER"),
    NUMBER("NUMBER"),
    BOOLEAN("BOOLEAN"),
    ARRAY("ARRAY"),
    OBJECT("OBJECT")
}

data class GenAiSchema(
    val type: GenAiType,
    val description: String? = null,
    val properties: Map<String, GenAiSchema>? = null,
    val required: List<String>? = null,
    val items: GenAiSchema? = null
) {
    fun toJson(): JSONObject = JSONObject().apply {
        put("type", type.jsonString)
        description?.let { put("description", it) }
        properties?.let { props ->
            val propsObj = JSONObject()
            props.forEach { (key, schema) ->
                propsObj.put(key, schema.toJson())
            }
            put("properties", propsObj)
        }
        required?.let { reqList ->
            val reqArray = JSONArray()
            reqList.forEach { reqArray.put(it) }
            put("required", reqArray)
        }
        items?.let { put("items", it.toJson()) }
    }

    companion object {
        fun string(description: String) = GenAiSchema(type = GenAiType.STRING, description = description)
        fun integer(description: String) = GenAiSchema(type = GenAiType.INTEGER, description = description)
        fun boolean(description: String) = GenAiSchema(type = GenAiType.BOOLEAN, description = description)
        fun obj(properties: Map<String, GenAiSchema>, required: List<String> = emptyList()) =
            GenAiSchema(type = GenAiType.OBJECT, properties = properties, required = required)
    }
}

data class GenAiFunctionDeclaration(
    val name: String,
    val description: String,
    val parameters: GenAiSchema
) {
    fun toJson(): JSONObject = JSONObject().apply {
        put("name", name)
        put("description", description)
        put("parameters", parameters.toJson())
    }
}

data class GenAiTool(
    val functionDeclarations: List<GenAiFunctionDeclaration> = emptyList(),
    val enableGoogleSearchGrounding: Boolean = false
) {
    fun toJson(): JSONObject = JSONObject().apply {
        if (functionDeclarations.isNotEmpty()) {
            val funcArray = JSONArray()
            functionDeclarations.forEach { funcArray.put(it.toJson()) }
            put("functionDeclarations", funcArray)
        }
        if (enableGoogleSearchGrounding) {
            put("googleSearch", JSONObject())
        }
    }
}

data class GenAiFunctionCall(
    val name: String,
    val args: Map<String, Any?>
)

data class GenAiFunctionResponse(
    val name: String,
    val response: Map<String, Any?>
) {
    fun toJson(): JSONObject = JSONObject().apply {
        put("name", name)
        put("response", JSONObject(response))
    }
}

/**
 * Functional interface for executing a declared Google Generative AI tool.
 */
interface IGenAiFunctionHandler {
    val declaration: GenAiFunctionDeclaration
    suspend fun execute(args: Map<String, Any?>, isArabic: Boolean): GenAiExecutionResult
}

data class GenAiExecutionResult(
    val toolName: String,
    val icon: String,
    val displayArg: String,
    val resultPreview: String,
    val responseData: Map<String, Any?>,
    val synthesizedSpeechAnswer: String
)
