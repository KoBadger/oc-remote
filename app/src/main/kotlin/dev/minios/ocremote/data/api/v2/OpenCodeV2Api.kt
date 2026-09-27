package dev.minios.ocremote.data.api.v2

import dev.minios.ocremote.data.api.ServerConnection
import dev.minios.ocremote.data.api.ServerAuthenticationException
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.plugins.timeout
import io.ktor.client.plugins.websocket.ClientWebSocketSession
import io.ktor.client.plugins.websocket.webSocketSession
import io.ktor.client.request.HttpRequestBuilder
import io.ktor.client.request.delete
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.request.parameter
import io.ktor.client.request.patch
import io.ktor.client.request.post
import io.ktor.client.request.put
import io.ktor.client.request.setBody
import io.ktor.client.request.url
import io.ktor.client.statement.HttpResponse
import io.ktor.client.statement.bodyAsText
import io.ktor.client.statement.readBytes
import io.ktor.http.ContentType
import io.ktor.http.HttpMethod
import io.ktor.http.contentType
import io.ktor.http.isSuccess
import io.ktor.websocket.Frame
import io.ktor.websocket.readText
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Which server contract a configured OpenCode server speaks.
 *
 * OpenCode V2 replaced the V1 server API (`/session`, `/global/health`) with an
 * `/api` surface. We detect it at connect time so both can be supported.
 */
enum class ServerApiVersion { V2, V1, UNKNOWN }

class V2HttpException(val statusCode: Int, val errorBody: String) :
    Exception("OpenCode V2 request failed (HTTP $statusCode)")

/**
 * OpenCode V2 REST client.
 *
 * Stateless: every call takes a [ServerConnection], so one instance serves many
 * servers. Mirrors [dev.minios.ocremote.data.api.OpenCodeApi] but targets the
 * `/api` V2 contract. See docs/v2/MIGRATION.md.
 */
@Singleton
class OpenCodeV2Api @Inject constructor(
    private val httpClient: HttpClient,
    private val json: Json,
) {
    /**
     * Detects whether the server speaks the V2 `/api` contract.
     * `/api/info` 2xx or 401/403 means V2; otherwise `/global/health` means V1.
     */
    suspend fun detectVersion(conn: ServerConnection): ServerApiVersion {
        // Prefer the V1 health endpoint: 1.18.x serves BOTH the V1 routes and an
        // experimental /api surface, and its /api/info falls through to the web UI HTML.
        val health = probe("${conn.baseUrl}/global/health", conn)
        if (health != null && health.status.isSuccess() && health.looksLikeJson()) {
            return ServerApiVersion.V1
        }
        val info = probe("${conn.baseUrl}/api/info", conn)
        if (info != null && info.status.isSuccess() && info.looksLikeJson()) {
            return ServerApiVersion.V2
        }
        // Reached but gated by auth: the route that exists tells us the version.
        if (health != null && (health.status.value == 401 || health.status.value == 403)) {
            return ServerApiVersion.V1
        }
        if (info != null && (info.status.value == 401 || info.status.value == 403)) {
            return ServerApiVersion.V2
        }
        return ServerApiVersion.UNKNOWN
    }

    private fun HttpResponse.looksLikeJson(): Boolean =
        headers["content-type"]?.contains("application/json", ignoreCase = true) == true

    private suspend fun probe(url: String, conn: ServerConnection): HttpResponse? = runCatching {
        httpClient.get(url) {
            conn.authHeader?.let { header("Authorization", it) }
            timeout { requestTimeoutMillis = 8_000; connectTimeoutMillis = 5_000 }
        }
    }.getOrNull()

    // ---- Infrastructure ----------------------------------------------------

    private fun HttpRequestBuilder.v2Scope(conn: ServerConnection, directory: String? = null) {
        conn.authHeader?.let { header("Authorization", it) }
        directory?.let { header("x-opencode-directory", it) }
    }

    private suspend fun ensureSuccess(response: HttpResponse, op: String): HttpResponse {
        if (response.status.value == 401 || response.status.value == 403) {
            throw ServerAuthenticationException(response.status.value)
        }
        if (!response.status.isSuccess()) {
            throw V2HttpException(response.status.value, runCatching { response.bodyAsText() }.getOrDefault(""))
        }
        return response
    }

    // ---- Server / projects -------------------------------------------------

    suspend fun getInfo(conn: ServerConnection): V2ServerInfo =
        ensureSuccess(httpClient.get("${conn.baseUrl}/api/info") { v2Scope(conn) }, "info").body()

    suspend fun listProjects(conn: ServerConnection): List<V2Project> =
        ensureSuccess(httpClient.get("${conn.baseUrl}/api/project") { v2Scope(conn) }, "project.list").body()

    // ---- Agents / commands / models / providers ----------------------------

    suspend fun listAgents(conn: ServerConnection, directory: String? = null): List<V2Agent> =
        ensureSuccess(httpClient.get("${conn.baseUrl}/api/agent") { v2Scope(conn, directory) }, "agent.list")
            .body<V2Located<List<V2Agent>>>().data

    suspend fun listCommands(conn: ServerConnection, directory: String? = null): List<V2Command> =
        ensureSuccess(httpClient.get("${conn.baseUrl}/api/command") { v2Scope(conn, directory) }, "command.list")
            .body<V2Located<List<V2Command>>>().data

    suspend fun listModels(conn: ServerConnection, directory: String? = null): List<V2Model> =
        ensureSuccess(httpClient.get("${conn.baseUrl}/api/model") { v2Scope(conn, directory) }, "model.list")
            .body<V2Located<List<V2Model>>>().data

    suspend fun getDefaultModel(conn: ServerConnection, directory: String? = null): V2Model? =
        ensureSuccess(httpClient.get("${conn.baseUrl}/api/model/default") { v2Scope(conn, directory) }, "model.default")
            .body<V2Located<V2Model?>>().data

    suspend fun listProviders(conn: ServerConnection, directory: String? = null): List<V2Provider> =
        ensureSuccess(httpClient.get("${conn.baseUrl}/api/provider") { v2Scope(conn, directory) }, "provider.list")
            .body<V2Located<List<V2Provider>>>().data

    // ---- Sessions ----------------------------------------------------------

    suspend fun listSessions(
        conn: ServerConnection,
        directory: String? = null,
        parentId: String? = null,
        limit: Int? = null,
        order: String? = null,
        search: String? = null,
        cursor: String? = null,
    ): V2Page<V2Session> =
        ensureSuccess(httpClient.get("${conn.baseUrl}/api/session") {
            v2Scope(conn, directory)
            directory?.let { parameter("directory", it) }
            parentId?.let { parameter("parentID", it) }
            limit?.let { parameter("limit", it) }
            order?.let { parameter("order", it) }
            search?.let { parameter("search", it) }
            cursor?.let { parameter("cursor", it) }
        }, "session.list").body()

    suspend fun listActiveSessions(
        conn: ServerConnection,
        directory: String? = null,
    ): Map<String, V2ActiveSession> =
        ensureSuccess(httpClient.get("${conn.baseUrl}/api/session/active") {
            v2Scope(conn, directory)
            directory?.let { parameter("directory", it) }
        }, "session.active").body<V2Envelope<Map<String, V2ActiveSession>>>().data

    suspend fun getSession(conn: ServerConnection, sessionId: String, directory: String? = null): V2Session =
        ensureSuccess(httpClient.get("${conn.baseUrl}/api/session/$sessionId") { v2Scope(conn, directory) }, "session.get")
            .body<V2Envelope<V2Session>>().data

    suspend fun createSession(
        conn: ServerConnection,
        request: V2CreateSessionRequest,
        directory: String? = null,
    ): V2Session = ensureSuccess(httpClient.post("${conn.baseUrl}/api/session") {
        v2Scope(conn, directory)
        contentType(ContentType.Application.Json)
        setBody(request)
    }, "session.create").body<V2Envelope<V2Session>>().data

    suspend fun updateSession(
        conn: ServerConnection,
        sessionId: String,
        request: V2UpdateSessionRequest,
        directory: String? = null,
    ): V2Session = ensureSuccess(httpClient.patch("${conn.baseUrl}/api/session/$sessionId") {
        v2Scope(conn, directory)
        contentType(ContentType.Application.Json)
        setBody(request)
    }, "session.update").body<V2Envelope<V2Session>>().data

    suspend fun deleteSession(conn: ServerConnection, sessionId: String, directory: String? = null): Boolean =
        ensureSuccess(httpClient.delete("${conn.baseUrl}/api/session/$sessionId") { v2Scope(conn, directory) }, "session.remove")
            .status.isSuccess()

    // ---- Messages ----------------------------------------------------------

    suspend fun listMessages(
        conn: ServerConnection,
        sessionId: String,
        directory: String? = null,
        limit: Int? = null,
        cursor: String? = null,
    ): V2Page<V2Message> =
        ensureSuccess(httpClient.get("${conn.baseUrl}/api/session/$sessionId/message") {
            v2Scope(conn, directory)
            directory?.let { parameter("directory", it) }
            limit?.let { parameter("limit", it) }
            cursor?.let { parameter("cursor", it) }
        }, "session.message.list").body()

    suspend fun getMessage(
        conn: ServerConnection,
        sessionId: String,
        messageId: String,
        directory: String? = null,
    ): V2Message = ensureSuccess(
        httpClient.get("${conn.baseUrl}/api/session/$sessionId/message/$messageId") { v2Scope(conn, directory) },
        "session.message.get",
    ).body<V2Envelope<V2Message>>().data

    // ---- Prompt / control --------------------------------------------------

    suspend fun prompt(
        conn: ServerConnection,
        sessionId: String,
        request: V2PromptRequest,
        directory: String? = null,
    ): V2Message = ensureSuccess(httpClient.post("${conn.baseUrl}/api/session/$sessionId/prompt") {
        v2Scope(conn, directory)
        contentType(ContentType.Application.Json)
        setBody(request)
    }, "session.prompt").body<V2Envelope<V2Message>>().data

    suspend fun interrupt(conn: ServerConnection, sessionId: String, directory: String? = null): Boolean =
        ensureSuccess(httpClient.post("${conn.baseUrl}/api/session/$sessionId/interrupt") { v2Scope(conn, directory) }, "session.interrupt")
            .status.isSuccess()

    suspend fun switchModel(
        conn: ServerConnection,
        sessionId: String,
        model: V2ModelRef,
        directory: String? = null,
    ): Boolean = ensureSuccess(httpClient.post("${conn.baseUrl}/api/session/$sessionId/model") {
        v2Scope(conn, directory)
        contentType(ContentType.Application.Json)
        setBody(V2SwitchModelRequest(model))
    }, "session.switchModel").status.isSuccess()

    suspend fun switchAgent(
        conn: ServerConnection,
        sessionId: String,
        agent: String,
        directory: String? = null,
    ): Boolean = ensureSuccess(httpClient.post("${conn.baseUrl}/api/session/$sessionId/agent") {
        v2Scope(conn, directory)
        contentType(ContentType.Application.Json)
        setBody(V2SwitchAgentRequest(agent))
    }, "session.switchAgent").status.isSuccess()

    suspend fun runCommand(
        conn: ServerConnection,
        sessionId: String,
        request: V2CommandRequest,
        directory: String? = null,
    ): Boolean = ensureSuccess(httpClient.post("${conn.baseUrl}/api/session/$sessionId/command") {
        v2Scope(conn, directory)
        contentType(ContentType.Application.Json)
        setBody(request)
    }, "session.command").status.isSuccess()

    suspend fun runShell(
        conn: ServerConnection,
        sessionId: String,
        request: V2ShellRequest,
        directory: String? = null,
    ): Boolean = ensureSuccess(httpClient.post("${conn.baseUrl}/api/session/$sessionId/shell") {
        v2Scope(conn, directory)
        contentType(ContentType.Application.Json)
        setBody(request)
    }, "session.shell").status.isSuccess()

    suspend fun compact(conn: ServerConnection, sessionId: String, directory: String? = null): Boolean =
        ensureSuccess(httpClient.post("${conn.baseUrl}/api/session/$sessionId/compact") { v2Scope(conn, directory) }, "session.compact")
            .status.isSuccess()

    // ---- Permissions / forms ----------------------------------------------

    suspend fun listPermissions(conn: ServerConnection, directory: String? = null): List<V2PermissionRequest> =
        ensureSuccess(httpClient.get("${conn.baseUrl}/api/permission/request") { v2Scope(conn, directory) }, "permission.request.list")
            .body<V2Located<List<V2PermissionRequest>>>().data

    suspend fun replyPermission(
        conn: ServerConnection,
        sessionId: String,
        requestId: String,
        decision: String,
        message: String? = null,
        directory: String? = null,
    ): Boolean = ensureSuccess(
        httpClient.post("${conn.baseUrl}/api/session/$sessionId/permission/$requestId/reply") {
            v2Scope(conn, directory)
            contentType(ContentType.Application.Json)
            setBody(V2PermissionReplyRequest(decision = decision, message = message))
        },
        "session.permission.reply",
    ).status.isSuccess()

    suspend fun listForms(conn: ServerConnection, directory: String? = null): List<V2Form> =
        ensureSuccess(httpClient.get("${conn.baseUrl}/api/form") { v2Scope(conn, directory) }, "form.list")
            .body<V2Located<List<V2Form>>>().data

    suspend fun replyForm(
        conn: ServerConnection,
        sessionId: String,
        formId: String,
        answer: Map<String, JsonElement>,
        directory: String? = null,
    ): Boolean = ensureSuccess(
        httpClient.post("${conn.baseUrl}/api/session/$sessionId/form/$formId/reply") {
            v2Scope(conn, directory)
            contentType(ContentType.Application.Json)
            setBody(V2FormReplyRequest(answer))
        },
        "session.form.reply",
    ).status.isSuccess()

    // ---- Filesystem --------------------------------------------------------

    suspend fun listDirectory(
        conn: ServerConnection,
        path: String = "",
        directory: String? = null,
    ): List<V2FileEntry> = ensureSuccess(httpClient.get("${conn.baseUrl}/api/fs/list") {
        v2Scope(conn, directory)
        parameter("path", path)
        directory?.let { parameter("directory", it) }
    }, "fs.list").body<V2Located<List<V2FileEntry>>>().data

    suspend fun findFiles(
        conn: ServerConnection,
        query: String,
        directory: String? = null,
    ): List<V2FileEntry> = ensureSuccess(httpClient.get("${conn.baseUrl}/api/fs/find") {
        v2Scope(conn, directory)
        parameter("query", query)
        directory?.let { parameter("directory", it) }
    }, "fs.find").body<V2Located<List<V2FileEntry>>>().data

    /** Reads a file. The path is sent unencoded after `/api/fs/read/`. */
    suspend fun readFile(conn: ServerConnection, path: String, directory: String? = null): ByteArray =
        ensureSuccess(httpClient.get("${conn.baseUrl}/api/fs/read/$path") {
            v2Scope(conn, directory)
            directory?.let { parameter("directory", it) }
        }, "fs.read").readBytes()

    // ---- PTY ---------------------------------------------------------------

    suspend fun listPtys(conn: ServerConnection, directory: String? = null): List<V2Pty> =
        ensureSuccess(httpClient.get("${conn.baseUrl}/api/pty") { v2Scope(conn, directory) }, "pty.list")
            .body<V2Located<List<V2Pty>>>().data

    suspend fun createPty(
        conn: ServerConnection,
        title: String? = null,
        cwd: String? = null,
        directory: String? = null,
    ): V2Pty = ensureSuccess(httpClient.post("${conn.baseUrl}/api/pty") {
        v2Scope(conn, directory)
        contentType(ContentType.Application.Json)
        setBody(buildMap<String, Any> {
            title?.let { put("title", it) }
            cwd?.let { put("cwd", it) }
        })
    }, "pty.create").body<V2Located<V2Pty>>().data

    suspend fun updatePtySize(
        conn: ServerConnection,
        ptyId: String,
        cols: Int,
        rows: Int,
        directory: String? = null,
    ): Boolean = ensureSuccess(httpClient.put("${conn.baseUrl}/api/pty/$ptyId") {
        v2Scope(conn, directory)
        contentType(ContentType.Application.Json)
        setBody(mapOf("size" to mapOf("rows" to rows, "cols" to cols)))
    }, "pty.update").status.isSuccess()

    suspend fun removePty(conn: ServerConnection, ptyId: String, directory: String? = null): Boolean =
        ensureSuccess(httpClient.delete("${conn.baseUrl}/api/pty/$ptyId") { v2Scope(conn, directory) }, "pty.remove")
            .status.isSuccess()

    suspend fun openPtySocket(
        conn: ServerConnection,
        ptyId: String,
        directory: String? = null,
    ): ClientWebSocketSession {
        val wsBase = when {
            conn.baseUrl.startsWith("https://") -> conn.baseUrl.replaceFirst("https://", "wss://")
            conn.baseUrl.startsWith("http://") -> conn.baseUrl.replaceFirst("http://", "ws://")
            else -> conn.baseUrl
        }
        return httpClient.webSocketSession {
            method = HttpMethod.Get
            url("$wsBase/api/pty/$ptyId/connect")
            conn.authHeader?.let { header("Authorization", it) }
            directory?.let { header("x-opencode-directory", it) }
        }
    }

    // ---- Config ------------------------------------------------------------

    /** `GET /api/config` returns the merged config as an array of entries. */
    suspend fun getConfig(conn: ServerConnection, directory: String? = null): List<JsonElement> =
        ensureSuccess(httpClient.get("${conn.baseUrl}/api/config") { v2Scope(conn, directory) }, "config.get").body()

    /** `PATCH /api/experimental/config` updates global configuration. */
    suspend fun updateGlobalConfig(
        conn: ServerConnection,
        patch: JsonElement,
        directory: String? = null,
    ): Boolean = ensureSuccess(httpClient.patch("${conn.baseUrl}/api/experimental/config") {
        v2Scope(conn, directory)
        contentType(ContentType.Application.Json)
        setBody(patch)
    }, "experimental.config.update").status.isSuccess()

    /** Drains frames from a PTY socket; the existing PtySocket helper can wrap this. */
    suspend fun readPtyText(session: ClientWebSocketSession, onText: suspend (String) -> Unit) {
        for (frame in session.incoming) {
            when (frame) {
                is Frame.Text -> onText(frame.readText())
                is Frame.Binary -> {
                    val data = frame.data
                    if (data.isEmpty() || data[0].toInt() == 0) continue
                    onText(data.toString(Charsets.UTF_8))
                }
                else -> Unit
            }
        }
    }
}
