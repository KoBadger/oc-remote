package dev.minios.ocremote.data.api.v2

import dev.minios.ocremote.domain.model.Message
import dev.minios.ocremote.domain.model.Part
import dev.minios.ocremote.domain.model.SseEvent
import dev.minios.ocremote.domain.model.ToolState
import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class V2ParsingTest {

    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
        coerceInputValues = true
        explicitNulls = false
    }

    @Test
    fun `session list page deserializes`() {
        val raw = """
            {"data":[{"id":"ses_1","projectID":"prj_1","parentID":null,"title":"Hello",
            "cost":0.1,"tokens":{"input":10,"output":20,"reasoning":0,"cache":{"read":0,"write":0}},
            "time":{"created":1000.0,"updated":2000.0},"location":{"directory":"/tmp"}}],
            "cursor":{"previous":null,"next":"c2"}}
        """.trimIndent()

        val page = json.decodeFromString(V2Page.serializer(V2Session.serializer()), raw)
        assertEquals(1, page.data.size)
        assertEquals("ses_1", page.data[0].id)
        assertEquals("prj_1", page.data[0].projectId)
        assertEquals("c2", page.cursor?.next)
    }

    @Test
    fun `assistant message maps to domain with inline parts`() {
        val raw = """
            {"id":"msg_1","type":"assistant","agent":"build","model":{"id":"claude","providerID":"anthropic"},
            "time":{"created":1000.0,"completed":2000.0},
            "content":[
              {"type":"text","text":"hi"},
              {"type":"reasoning","text":"thinking"},
              {"type":"tool","id":"call_1","name":"bash","state":{"status":"completed","input":{"cmd":"ls"},
               "content":[{"type":"text","text":"file.txt"}]}}
            ]}
        """.trimIndent()

        val message = json.decodeFromString(V2Message.serializer(), raw)
        val domain = V2Mappers.messageToDomain(message, "ses_1")
        assertNotNull(domain)
        assertTrue(domain!!.info is Message.Assistant)
        assertEquals("claude", (domain.info as Message.Assistant).modelId)
        assertEquals(3, domain.parts.size)
        assertEquals("hi", (domain.parts[0] as Part.Text).text)
        assertEquals("thinking", (domain.parts[1] as Part.Reasoning).text)

        val tool = domain.parts[2] as Part.Tool
        assertEquals("bash", tool.tool)
        val completed = tool.state as ToolState.Completed
        assertEquals("file.txt", completed.output)
    }

    @Test
    fun `user message maps to text part`() {
        val raw = """{"id":"msg_u","type":"user","time":{"created":1.0},"text":"hello"}"""
        val message = json.decodeFromString(V2Message.serializer(), raw)
        val domain = V2Mappers.messageToDomain(message, "ses_1")!!
        assertTrue(domain.info is Message.User)
        assertEquals("hello", (domain.parts.single() as Part.Text).text)
    }

    @Test
    fun `v2 session maps to domain session`() {
        val v2 = V2Session(
            id = "ses_x",
            projectId = "prj_x",
            title = "T",
            time = V2SessionTime(created = 12.0, updated = 34.0, archived = 56.0),
        )
        val domain = V2Mappers.sessionToDomain(v2)
        assertEquals("ses_x", domain.id)
        assertEquals(12L, domain.createdAt)
        assertTrue(domain.isArchived)
    }

    @Test
    fun `session text delta event maps to existing sse event`() {
        val parser = V2EventParser(json)
        val raw = """
            {"type":"session.text.delta","properties":
              {"sessionID":"ses_1","assistantMessageID":"msg_1","textID":"txt_1","delta":"hey"}}
        """.trimIndent()

        val scoped = parser.parse(raw)
        assertNotNull(scoped)
        val event = scoped!!.event
        assertTrue(event is SseEvent.NextTextDelta)
        event as SseEvent.NextTextDelta
        assertEquals("ses_1", event.sessionId)
        assertEquals("msg_1", event.messageId)
        assertEquals("hey", event.delta)
    }

    @Test
    fun `permission asked maps action and resources`() {
        val parser = V2EventParser(json)
        val raw = """
            {"type":"permission.asked","properties":
              {"id":"per_1","sessionID":"ses_1","action":"shell","resources":["rm -rf /"],
               "save":["always"],"source":{"type":"tool","messageID":"msg_1","id":"call_1"}}}
        """.trimIndent()

        val event = parser.parse(raw)!!.event as SseEvent.PermissionAsked
        assertEquals("shell", event.permission)
        assertEquals(listOf("rm -rf /"), event.patterns)
        assertEquals("msg_1", event.tool?.messageId)
    }

    @Test
    fun `event envelope form with data string parses`() {
        val parser = V2EventParser(json)
        val raw = """
            {"id":"e1","event":"session.tool.called",
             "data":"{\"sessionID\":\"ses_1\",\"assistantMessageID\":\"msg_1\",\"callID\":\"c1\",\"tool\":\"read\",\"input\":{}}"}
        """.trimIndent()

        val event = parser.parse(raw)!!.event
        assertTrue(event is SseEvent.NextToolCalled)
        assertEquals("read", (event as SseEvent.NextToolCalled).tool)
    }
}
