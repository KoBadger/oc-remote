package dev.minios.ocremote.data.api.v2

import dev.minios.ocremote.domain.model.Message
import dev.minios.ocremote.domain.model.MessageWithParts
import dev.minios.ocremote.domain.model.Part
import dev.minios.ocremote.domain.model.Session
import dev.minios.ocremote.domain.model.TimeInfo
import dev.minios.ocremote.domain.model.ToolState
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

/**
 * Translates OpenCode V2 wire values into the app's existing V1-shaped domain
 * models. This lets the reducer and Compose UI keep working during the migration;
 * a later phase can adopt V2-native models and drop these adapters.
 *
 * See docs/v2/SHAPES.md for the source shapes.
 */
object V2Mappers {

    fun sessionToDomain(v2: V2Session): Session = Session(
        id = v2.id,
        slug = "",
        projectId = v2.projectId,
        directory = v2.subpath ?: "",
        workspaceId = null,
        parentId = v2.parentId,
        title = v2.title,
        version = "",
        time = Session.Time(
            created = v2.time.created.toLong(),
            updated = v2.time.updated.toLong(),
            compacting = null,
            archived = v2.time.archived?.toLong(),
        ),
        summary = null,
        share = null,
        permission = null,
        revert = null,
    )

    /**
     * Converts a V2 message (with inline content) into the V1 `MessageWithParts`.
     * Returns null for message kinds the current UI does not render yet
     * (shell, skill, compaction, idle, agent/model-switched, …).
     */
    fun messageToDomain(v2: V2Message, sessionId: String): MessageWithParts? {
        return when (v2.type) {
            "user" -> userMessage(v2, sessionId)
            "assistant" -> assistantMessage(v2, sessionId)
            // System/synthetic carry visible text; surface them as a text-only message.
            "system", "synthetic" -> syntheticMessage(v2, sessionId)
            else -> null
        }
    }

    private fun userMessage(v2: V2Message, sessionId: String): MessageWithParts {
        val info = Message.User(
            id = v2.id,
            sessionId = sessionId,
            time = TimeInfo(created = v2.time.created.toLong()),
        )
        val parts = buildList {
            v2.text?.takeIf { it.isNotEmpty() }?.let {
                add(Part.Text(id = "${v2.id}-text-0", sessionId = sessionId, messageId = v2.id, text = it))
            }
            v2.files?.forEachIndexed { index, file ->
                add(
                    Part.File(
                        id = "${v2.id}-file-$index",
                        sessionId = sessionId,
                        messageId = v2.id,
                        mime = guessMime(file.name),
                        filename = file.name,
                        url = file.uri,
                    ),
                )
            }
        }
        return MessageWithParts(info, parts)
    }

    private fun syntheticMessage(v2: V2Message, sessionId: String): MessageWithParts {
        val info = Message.User(
            id = v2.id,
            sessionId = sessionId,
            time = TimeInfo(created = v2.time.created.toLong()),
        )
        val text = v2.text.orEmpty()
        val parts = if (text.isEmpty()) emptyList() else listOf(
            Part.Text(id = "${v2.id}-text-0", sessionId = sessionId, messageId = v2.id, text = text),
        )
        return MessageWithParts(info, parts)
    }

    private fun assistantMessage(v2: V2Message, sessionId: String): MessageWithParts {
        val info = Message.Assistant(
            id = v2.id,
            sessionId = sessionId,
            time = TimeInfo(
                created = v2.time.created.toLong(),
                completed = v2.time.completed?.toLong(),
            ),
            parentId = "",
            modelId = v2.model?.id,
            providerId = v2.model?.providerId,
            agent = v2.agent,
            cost = v2.cost,
            tokens = v2.tokens?.let { tokens ->
                Message.Assistant.Tokens(
                    input = tokens.input.toInt(),
                    output = tokens.output.toInt(),
                    reasoning = tokens.reasoning.toInt(),
                    cache = Message.Assistant.Tokens.Cache(
                        read = tokens.cache.read.toInt(),
                        write = tokens.cache.write.toInt(),
                    ),
                )
            },
            finish = v2.finish,
            error = v2.error?.let { errorInfo(it) },
            variant = v2.model?.variant,
        )

        val parts = buildList {
            v2.content.orEmpty().forEachIndexed { index, content ->
                when (content) {
                    is V2AssistantContent.Text -> add(
                        Part.Text(
                            id = "${v2.id}-text-$index",
                            sessionId = sessionId,
                            messageId = v2.id,
                            text = content.text,
                        ),
                    )
                    is V2AssistantContent.Reasoning -> add(
                        Part.Reasoning(
                            id = "${v2.id}-reasoning-$index",
                            sessionId = sessionId,
                            messageId = v2.id,
                            text = content.text,
                        ),
                    )
                    is V2AssistantContent.Tool -> add(
                        Part.Tool(
                            id = content.id.ifBlank { "${v2.id}-tool-$index" },
                            sessionId = sessionId,
                            messageId = v2.id,
                            callId = content.id,
                            tool = content.name,
                            state = toolState(content.state),
                        ),
                    )
                    is V2AssistantContent.Unknown -> Unit
                }
            }
        }
        return MessageWithParts(info, parts)
    }

    private fun toolState(state: V2ToolState?): ToolState = when (state) {
        is V2ToolState.Streaming -> ToolState.Pending(input = emptyMap(), raw = state.input)
        is V2ToolState.Running -> ToolState.Running(
            input = state.input.mapValues { it.value },
            metadata = state.metadata,
        )
        is V2ToolState.Completed -> ToolState.Completed(
            input = state.input.mapValues { it.value },
            output = contentToText(state.content),
            metadata = state.metadata,
        )
        is V2ToolState.Error -> ToolState.Error(
            input = state.input.mapValues { it.value },
            error = elementToText(state.error) ?: contentToText(state.content.orEmpty()),
            metadata = state.metadata,
        )
        is V2ToolState.Unknown, null -> ToolState.Pending()
    }

    private fun errorInfo(element: JsonElement): Message.Assistant.ErrorInfo {
        val name = (element as? JsonObject)?.get("name")?.jsonPrimitive?.contentOrNull ?: "Error"
        return Message.Assistant.ErrorInfo(name = name, data = element)
    }

    /** Joins text blocks from a V2 `Tool.Content[]`. */
    private fun contentToText(content: List<JsonElement>): String =
        content.joinToString("\n") { element ->
            when (element) {
                is JsonPrimitive -> element.contentOrNull.orEmpty()
                is JsonObject -> element["text"]?.jsonPrimitive?.contentOrNull
                    ?: element["content"]?.jsonPrimitive?.contentOrNull
                    ?: element.toString()
                is JsonArray -> element.joinToString(" ") { it.jsonPrimitive.contentOrNull.orEmpty() }
                else -> element.toString()
            }
        }.trim()

    private fun elementToText(element: JsonElement?): String? = when (element) {
        null -> null
        is JsonPrimitive -> element.contentOrNull
        is JsonObject -> element["message"]?.jsonPrimitive?.contentOrNull
            ?: element["name"]?.jsonPrimitive?.contentOrNull
        else -> element.toString()
    }

    private fun guessMime(name: String?): String = when (name?.substringAfterLast('.', "")?.lowercase()) {
        "png" -> "image/png"
        "jpg", "jpeg" -> "image/jpeg"
        "gif" -> "image/gif"
        "webp" -> "image/webp"
        "pdf" -> "application/pdf"
        "json" -> "application/json"
        else -> "application/octet-stream"
    }
}
