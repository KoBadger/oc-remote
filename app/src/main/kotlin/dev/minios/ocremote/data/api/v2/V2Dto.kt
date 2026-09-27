package dev.minios.ocremote.data.api.v2

import kotlinx.serialization.DeserializationStrategy
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonContentPolymorphicSerializer
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

/**
 * OpenCode V2 wire DTOs.
 *
 * Generated/derived from docs/v2/openapi.json (OpenCode 2.x). Shapes are intentionally
 * lenient (`ignoreUnknownKeys` is enabled globally) so the client tolerates 2.0.x drift.
 */

// ---- Envelopes -------------------------------------------------------------

/** `{ "data": ... }` response wrapper used by most V2 endpoints. */
@Serializable
data class V2Envelope<T>(val data: T)

/** `{ "location": ..., "data": ... }` wrapper used by location-scoped list endpoints. */
@Serializable
data class V2Located<T>(val data: T, val location: JsonElement? = null)

/** Cursor pagination block returned by list endpoints. */
@Serializable
data class V2Cursor(val previous: String? = null, val next: String? = null)

/** `{ "data": [...], "cursor": {...} }` page used by sessions and messages. */
@Serializable
data class V2Page<T>(val data: List<T> = emptyList(), val cursor: V2Cursor? = null)

// ---- Server ----------------------------------------------------------------

@Serializable
data class V2ServerInfo(
    val version: String = "",
    val pid: Long = 0,
    val urls: List<String> = emptyList(),
    val paths: V2ServerPaths = V2ServerPaths(),
)

@Serializable
data class V2ServerPaths(val tmp: String = "")

// ---- Shared refs -----------------------------------------------------------

@Serializable
data class V2ModelRef(
    val id: String = "",
    @SerialName("providerID") val providerId: String = "",
    val variant: String? = null,
)

@Serializable
data class V2TokenUsage(
    val input: Double = 0.0,
    val output: Double = 0.0,
    val reasoning: Double = 0.0,
    val cache: V2TokenCache = V2TokenCache(),
)

@Serializable
data class V2TokenCache(val read: Double = 0.0, val write: Double = 0.0)

// ---- Sessions --------------------------------------------------------------

@Serializable
data class V2Session(
    val id: String = "",
    @SerialName("parentID") val parentId: String? = null,
    @SerialName("projectID") val projectId: String = "",
    val agent: String? = null,
    val model: V2ModelRef? = null,
    val cost: Double = 0.0,
    val tokens: V2TokenUsage? = null,
    val outcome: String? = null,
    val time: V2SessionTime = V2SessionTime(),
    val title: String? = null,
    val subpath: String? = null,
    val metadata: JsonObject? = null,
    val permissions: List<JsonElement>? = null,
    val revert: JsonElement? = null,
    val location: JsonElement? = null,
)

@Serializable
data class V2SessionTime(
    val created: Double = 0.0,
    val updated: Double = 0.0,
    val idle: Double? = null,
    val viewed: Double? = null,
    val archived: Double? = null,
)

/** `GET /api/session/active` value: `{ "type": "running" }`. */
@Serializable
data class V2ActiveSession(val type: String = "idle")

// ---- Messages --------------------------------------------------------------

@Serializable
data class V2MessageTime(
    val created: Double = 0.0,
    val streamed: Double? = null,
    val completed: Double? = null,
)

/**
 * A V2 session message. V2 uses a flat `type` discriminator and stores assistant
 * content inline in [content] rather than emitting separate part records.
 */
@Serializable
data class V2Message(
    val id: String = "",
    val type: String = "",
    val metadata: JsonObject? = null,
    val time: V2MessageTime = V2MessageTime(),

    // user / system / synthetic
    val text: String? = null,
    val description: String? = null,
    val files: List<V2PromptFile>? = null,
    val agents: List<JsonElement>? = null,
    val skills: List<JsonElement>? = null,

    // assistant
    val agent: String? = null,
    val model: V2ModelRef? = null,
    val content: List<V2AssistantContent>? = null,
    val finish: String? = null,
    @SerialName("rawFinish") val rawFinish: String? = null,
    val cost: Double? = null,
    val tokens: V2TokenUsage? = null,
    val error: JsonElement? = null,
    val retry: JsonElement? = null,

    // idle
    val outcome: String? = null,
)

@Serializable(with = V2AssistantContentSerializer::class)
sealed class V2AssistantContent {
    @Serializable
    data class Text(val text: String = "") : V2AssistantContent()

    @Serializable
    data class Reasoning(
        val text: String = "",
        val time: V2MessageTime? = null,
    ) : V2AssistantContent()

    @Serializable
    data class Tool(
        val id: String = "",
        val name: String = "",
        val executed: Boolean = false,
        val state: V2ToolState? = null,
        val time: V2MessageTime? = null,
    ) : V2AssistantContent()

    @Serializable
    data class Unknown(val type: String = "") : V2AssistantContent()
}

object V2AssistantContentSerializer :
    JsonContentPolymorphicSerializer<V2AssistantContent>(V2AssistantContent::class) {
    override fun selectDeserializer(element: JsonElement): DeserializationStrategy<V2AssistantContent> =
        when (element.jsonObject["type"]?.jsonPrimitive?.content) {
            "text" -> V2AssistantContent.Text.serializer()
            "reasoning" -> V2AssistantContent.Reasoning.serializer()
            "tool" -> V2AssistantContent.Tool.serializer()
            else -> V2AssistantContent.Unknown.serializer()
        }
}

@Serializable(with = V2ToolStateSerializer::class)
sealed class V2ToolState {
    @Serializable
    data class Streaming(val input: String = "") : V2ToolState()

    @Serializable
    data class Running(
        val input: JsonObject = JsonObject(emptyMap()),
        val metadata: JsonObject? = null,
    ) : V2ToolState()

    @Serializable
    data class Completed(
        val input: JsonObject = JsonObject(emptyMap()),
        val content: List<JsonElement> = emptyList(),
        val metadata: JsonObject? = null,
    ) : V2ToolState()

    @Serializable
    data class Error(
        val input: JsonObject = JsonObject(emptyMap()),
        val error: JsonElement? = null,
        val content: List<JsonElement>? = null,
        val metadata: JsonObject? = null,
    ) : V2ToolState()

    @Serializable
    data class Unknown(val status: String = "") : V2ToolState()
}

object V2ToolStateSerializer : JsonContentPolymorphicSerializer<V2ToolState>(V2ToolState::class) {
    override fun selectDeserializer(element: JsonElement): DeserializationStrategy<V2ToolState> =
        when (element.jsonObject["status"]?.jsonPrimitive?.content) {
            "streaming" -> V2ToolState.Streaming.serializer()
            "running" -> V2ToolState.Running.serializer()
            "completed" -> V2ToolState.Completed.serializer()
            "error" -> V2ToolState.Error.serializer()
            else -> V2ToolState.Unknown.serializer()
        }
}

@Serializable
data class V2PromptFile(
    val uri: String = "",
    val name: String? = null,
    val description: String? = null,
)

// ---- Agents / commands / models / providers --------------------------------

@Serializable
data class V2Agent(
    val id: String = "",
    val name: String = "",
    val model: V2ModelRef? = null,
    val description: String? = null,
    val mode: String = "primary",
    val hidden: Boolean = false,
    val color: String? = null,
    val steps: Int? = null,
)

@Serializable
data class V2Command(
    val name: String = "",
    val description: String? = null,
)

@Serializable
data class V2Model(
    val id: String = "",
    @SerialName("modelID") val modelId: String = "",
    @SerialName("providerID") val providerId: String = "",
    val name: String = "",
    val family: String? = null,
    val status: String = "active",
    val enabled: Boolean = true,
    val capabilities: JsonObject? = null,
    val variants: List<V2ModelVariant>? = null,
)

@Serializable
data class V2ModelVariant(val id: String = "")

@Serializable
data class V2Provider(
    val id: String = "",
    val name: String = "",
    val activation: String = "auto",
    val packageName: String? = null,
    val integrationId: String? = null,
)

@Serializable
data class V2Project(
    val id: String = "",
    val canonical: String = "",
    val name: String? = null,
    val vcs: String? = null,
    val sandboxes: List<String> = emptyList(),
)

// ---- Permissions / forms ---------------------------------------------------

@Serializable
data class V2PermissionRequest(
    val id: String = "",
    @SerialName("sessionID") val sessionId: String = "",
    val action: String = "",
    val resources: List<String> = emptyList(),
    val save: List<String> = emptyList(),
    val metadata: JsonObject? = null,
    val source: JsonElement? = null,
    val message: String? = null,
)

@Serializable
data class V2Form(
    val id: String = "",
    @SerialName("sessionID") val sessionId: String = "",
    val title: String = "",
    val metadata: JsonObject? = null,
    val fields: List<V2FormField> = emptyList(),
)

/** A form field is dynamic; keep the raw JSON and interpret by `type` in the UI. */
@Serializable
data class V2FormField(
    val key: String = "",
    val type: String = "",
    val title: String? = null,
    val description: String? = null,
    val required: Boolean = false,
    val hidden: Boolean = false,
    val options: List<V2FormOption> = emptyList(),
    val raw: JsonObject = JsonObject(emptyMap()),
)

@Serializable
data class V2FormOption(val label: String = "", val description: String? = null)

// ---- PTY / files -----------------------------------------------------------

@Serializable
data class V2Pty(
    val id: String = "",
    val title: String = "",
    val command: String = "",
    val args: List<String> = emptyList(),
    val cwd: String = "",
    val status: String = "running",
    val pid: Long = 0,
    val exitCode: Int? = null,
)

@Serializable
data class V2FileEntry(val path: String = "", val type: String = "file")

// ---- Request bodies --------------------------------------------------------

@Serializable
data class V2CreateSessionRequest(
    val id: String? = null,
    val title: String? = null,
    val agent: String? = null,
    val model: V2ModelRef? = null,
    val metadata: JsonObject? = null,
)

@Serializable
data class V2UpdateSessionRequest(
    val title: String? = null,
    val metadata: JsonObject? = null,
)

@Serializable
data class V2PromptRequest(
    val id: String? = null,
    val text: String,
    val files: List<V2PromptFile> = emptyList(),
    val delivery: String? = null,
    val resume: Boolean? = null,
)

@Serializable
data class V2SwitchModelRequest(val model: V2ModelRef)

@Serializable
data class V2SwitchAgentRequest(val agent: String)

@Serializable
data class V2CommandRequest(
    val name: String,
    val text: String,
    val files: List<V2PromptFile> = emptyList(),
    val delivery: String? = null,
)

@Serializable
data class V2ShellRequest(
    val id: String? = null,
    val command: String,
)

@Serializable
data class V2SyntheticRequest(
    val id: String? = null,
    val text: String,
    val description: String? = null,
)

@Serializable
data class V2PermissionReplyRequest(
    val decision: String,
    val message: String? = null,
)

@Serializable
data class V2FormReplyRequest(val answer: Map<String, JsonElement> = emptyMap())
