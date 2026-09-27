package dev.minios.ocremote.data.api.v2

import dev.minios.ocremote.data.api.ScopedSseEvent
import dev.minios.ocremote.data.api.parseSessionError
import dev.minios.ocremote.domain.model.Message
import dev.minios.ocremote.domain.model.Session
import dev.minios.ocremote.domain.model.SessionStatus
import dev.minios.ocremote.domain.model.SseEvent
import dev.minios.ocremote.domain.model.ToolRef
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.booleanOrNull
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.int
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.long
import kotlinx.serialization.json.longOrNull
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Parses OpenCode V2 `/api/event` frames into the app's existing [SseEvent] model.
 *
 * V2 renamed the streaming event family from `session.next.*` to `session.*` and
 * keeps `message.part.*` / `permission.*`. Mapping onto the existing sealed type
 * lets [dev.minios.ocremote.data.repository.EventReducer] stay unchanged while the
 * transport moves to V2.
 *
 * Frames are tolerant of both envelope shapes seen in the wild:
 *  - `{"type": "...", "properties": {...}}`
 *  - `{"event": "...", "data": "<json string or object>"}`
 */
@Singleton
class V2EventParser @Inject constructor(private val json: Json) {

    fun parse(raw: String): ScopedSseEvent? {
        val root = runCatching { json.parseToJsonElement(raw).jsonObject }.getOrNull() ?: return null

        var type = root["type"]?.jsonPrimitive?.contentOrNull
            ?: root["event"]?.jsonPrimitive?.contentOrNull
            ?: return null

        var props: JsonObject = when {
            root["properties"] is JsonObject -> root["properties"]!!.jsonObject
            root["data"] is JsonObject -> root["data"]!!.jsonObject
            root["data"] is JsonPrimitive ->
                runCatching { json.parseToJsonElement((root["data"] as JsonPrimitive).content).jsonObject }.getOrNull()
                    ?: JsonObject(emptyMap())
            else -> root
        }

        // Some V2 frames double-encode: {"event": "...", "data": "{\"type\": ..., \"properties\": ...}"}.
        val innerType = props["type"]?.jsonPrimitive?.contentOrNull
        if (innerType != null && props["properties"] is JsonObject) {
            type = innerType
            props = props["properties"]!!.jsonObject
        }

        val directory = root["directory"]?.jsonPrimitive?.contentOrNull
        val event = parseByType(type, props) ?: return null

        return ScopedSseEvent(
            event = event,
            directory = directory,
            projectId = root["project"]?.jsonPrimitive?.contentOrNull,
            workspaceId = root["workspace"]?.jsonPrimitive?.contentOrNull,
            eventId = root["id"]?.jsonPrimitive?.contentOrNull,
        )
    }

    private fun parseByType(type: String, props: JsonObject): SseEvent? = runCatching {
        when (type) {
            "server.connected" -> SseEvent.ServerConnected
            "server.heartbeat" -> SseEvent.ServerHeartbeat
            "server.instance.disposed" -> SseEvent.ServerInstanceDisposed(props.str("directory"))

            "session.created" -> SseEvent.SessionCreated(session(props))
            "session.deleted" -> SseEvent.SessionDeleted(session(props))
            "session.patched", "session.renamed", "session.moved", "session.metadata.updated" ->
                SseEvent.SessionUpdated(session(props))

            "session.status" -> SseEvent.SessionStatus(
                sessionId = props.str("sessionID"),
                status = status(props["status"]),
            )
            "session.execution.started" -> SseEvent.SessionStatus(props.str("sessionID"), SessionStatus.Busy)
            "session.execution.succeeded", "session.execution.interrupted" ->
                SseEvent.SessionStatus(props.str("sessionID"), SessionStatus.Idle)
            "session.execution.failed" -> parseSessionError(props, json)
            "session.retry.scheduled" -> SseEvent.SessionStatus(
                props.str("sessionID"),
                SessionStatus.Retry(
                    attempt = props["attempt"]?.jsonPrimitive?.int ?: 0,
                    message = props.str("message"),
                    next = props.long("at"),
                ),
            )
            "session.idle" -> SseEvent.SessionIdle(props.str("sessionID"))
            "session.error" -> parseSessionError(props, json)
            "session.compaction.started", "session.compaction.ended" -> SseEvent.SessionCompacted(props.str("sessionID"))

            "session.text.started" -> SseEvent.NextTextStarted(
                props.str("sessionID"), props.msgId(), props.str("textID"), props.long("timestamp"),
            )
            "session.text.delta" -> SseEvent.NextTextDelta(
                props.str("sessionID"), props.msgId(), props.str("textID"), props.str("delta"),
            )
            "session.text.ended" -> SseEvent.NextTextEnded(
                props.str("sessionID"), props.msgId(), props.str("textID"), props.str("text"), props.long("timestamp"),
            )
            "session.reasoning.started" -> SseEvent.NextReasoningStarted(
                props.str("sessionID"), props.msgId(), props.str("reasoningID"), props.long("timestamp"),
            )
            "session.reasoning.delta" -> SseEvent.NextReasoningDelta(
                props.str("sessionID"), props.msgId(), props.str("reasoningID"), props.str("delta"),
            )
            "session.reasoning.ended" -> SseEvent.NextReasoningEnded(
                props.str("sessionID"), props.msgId(), props.str("reasoningID"), props.str("text"), props.long("timestamp"),
            )
            "session.tool.input.started" -> SseEvent.NextToolInputStarted(
                props.str("sessionID"), props.msgId(), props.str("callID"), props.str("name"), props.long("timestamp"),
            )
            "session.tool.input.delta" -> SseEvent.NextToolInputDelta(
                props.str("sessionID"), props.msgId(), props.str("callID"), props.str("delta"),
            )
            "session.tool.input.ended" -> SseEvent.NextToolInputEnded(
                props.str("sessionID"), props.msgId(), props.str("callID"), props.str("text"),
            )
            "session.tool.called" -> SseEvent.NextToolCalled(
                props.str("sessionID"), props.msgId(), props.str("callID"), props.str("tool"),
                props["input"] ?: JsonObject(emptyMap()), props.long("timestamp"),
            )
            "session.tool.progress" -> SseEvent.NextToolProgress(
                props.str("sessionID"), props.msgId(), props.str("callID"),
                props["structured"] ?: JsonObject(emptyMap()), props["content"] ?: JsonArray(emptyList()), props.long("timestamp"),
            )
            "session.tool.success" -> SseEvent.NextToolSuccess(
                props.str("sessionID"), props.msgId(), props.str("callID"),
                props["structured"] ?: JsonObject(emptyMap()), props["content"] ?: JsonArray(emptyList()), props.long("timestamp"),
            )
            "session.tool.failed" -> SseEvent.NextToolFailed(
                props.str("sessionID"), props.msgId(), props.str("callID"),
                props["error"] ?: JsonObject(emptyMap()), props.long("timestamp"),
            )
            "session.step.started" -> SseEvent.NextStepStarted(
                props.str("sessionID"), props.msgId(), props.str("agent"),
                props["model"] ?: JsonObject(emptyMap()), props.long("timestamp"),
            )
            "session.step.ended" -> SseEvent.NextStepEnded(
                props.str("sessionID"), props.msgId(), props.str("finish"),
                props["cost"]?.jsonPrimitive?.contentOrNull?.toDoubleOrNull() ?: 0.0,
                props["tokens"] ?: JsonObject(emptyMap()), props.long("timestamp"),
            )
            "session.step.failed" -> SseEvent.NextStepFailed(
                props.str("sessionID"), props.msgId(), props["error"] ?: JsonObject(emptyMap()), props.long("timestamp"),
            )
            "session.agent.selected" -> SseEvent.NextAgentSwitched(
                props.str("sessionID"), props.msgId(), props.str("agent"),
            )
            "session.model.selected" -> SseEvent.NextModelSwitched(
                props.str("sessionID"), props.msgId(), props["model"] ?: JsonObject(emptyMap()),
            )
            "session.synthetic" -> SseEvent.NextSynthetic(
                props.str("sessionID"), props.msgId(), props.str("text"), props.long("timestamp"),
            )
            "session.shell.started" -> SseEvent.NextShellStarted(
                props.str("sessionID"), props.msgId(), props.str("callID"), props.str("command"), props.long("timestamp"),
            )
            "session.shell.ended" -> SseEvent.NextShellEnded(
                props.str("sessionID"), props.str("callID"), props.str("output"), props.long("timestamp"),
            )

            "message.updated" -> {
                val info = V2Mappers.messageToDomain(
                    json.decodeFromJsonElement(V2Message.serializer(), props["info"]?.jsonObject ?: props),
                    sessionId = props.str("sessionID"),
                )
                info?.let { SseEvent.MessageUpdated(it.info) }
            }
            "message.removed" -> SseEvent.MessageRemoved(props.str("sessionID"), props.str("messageID"))
            "message.part.delta" -> SseEvent.MessagePartDelta(
                props.str("sessionID"), props.str("messageID"), props.str("partID"),
                props.str("field", "text"), props.str("delta"),
            )
            "message.part.removed" -> SseEvent.MessagePartRemoved(
                props.str("sessionID"), props.str("messageID"), props.str("partID"),
            )

            "permission.asked" -> SseEvent.PermissionAsked(
                id = props.str("id"),
                sessionId = props.str("sessionID"),
                permission = props.str("action"),
                patterns = props.stringList("resources"),
                metadata = props["metadata"]?.jsonObject,
                always = props.stringList("save"),
                tool = props["source"]?.jsonObject?.let { src ->
                    ToolRef(messageId = src.str("messageID"), callId = src.str("id"))
                },
            )
            "permission.replied" -> SseEvent.PermissionReplied(props.str("sessionID"), props.str("requestID"))

            // V2 replaces Questions with Forms; form.created is surfaced as a question-shaped
            // event so the current activity queue can still display it (phase 4 adds a Form UI).
            "form.created" -> SseEvent.QuestionAsked(
                id = props.str("id"),
                sessionId = props.str("sessionID"),
                questions = props["fields"]?.jsonArray?.map { field ->
                    val obj = field.jsonObject
                    SseEvent.QuestionAsked.Question(
                        header = obj.str("title", obj.str("key")),
                        question = obj.str("title", obj.str("description")),
                        multiple = obj.str("type") == "multiselect",
                        custom = true,
                        options = obj["options"]?.jsonArray?.map { opt ->
                            SseEvent.QuestionAsked.Option(
                                label = opt.jsonObject.str("label"),
                                description = opt.jsonObject.str("description"),
                            )
                        } ?: emptyList(),
                    )
                } ?: emptyList(),
            )
            "form.replied", "form.cancelled", "form.settled" -> SseEvent.QuestionReplied(
                props.str("sessionID"), props.str("id"),
            )

            else -> null
        }
    }.getOrNull()

    // ---- helpers -----------------------------------------------------------

    private fun session(props: JsonObject): Session {
        val info = props["info"]?.jsonObject ?: props
        return V2Mappers.sessionToDomain(json.decodeFromJsonElement(V2Session.serializer(), info))
    }

    private fun status(element: JsonElement?): SessionStatus {
        val obj = element as? JsonObject
        return when (obj?.get("type")?.jsonPrimitive?.contentOrNull) {
            "busy", "running" -> SessionStatus.Busy
            "retry" -> SessionStatus.Retry(
                attempt = obj["attempt"]?.jsonPrimitive?.int ?: 0,
                message = obj.str("message"),
                next = obj.long("next"),
            )
            else -> SessionStatus.Idle
        }
    }

    private fun JsonObject.msgId(): String =
        this["assistantMessageID"]?.jsonPrimitive?.contentOrNull ?: str("messageID")

    private fun JsonObject.str(key: String, default: String = ""): String =
        this[key]?.jsonPrimitive?.contentOrNull ?: default

    private fun JsonObject.long(key: String, default: Long = 0L): Long =
        this[key]?.jsonPrimitive?.longOrNull ?: default

    private fun JsonObject.stringList(key: String): List<String> =
        this[key]?.jsonArray?.mapNotNull { it.jsonPrimitive.contentOrNull } ?: emptyList()
}
