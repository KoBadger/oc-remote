package dev.minios.ocremote.data.api.v2

import dev.minios.ocremote.data.api.AgentInfo
import dev.minios.ocremote.data.api.CommandInfo
import dev.minios.ocremote.data.api.FileContent
import dev.minios.ocremote.data.api.FileNode
import dev.minios.ocremote.data.api.MessagePage
import dev.minios.ocremote.data.api.ModelSelection
import dev.minios.ocremote.data.api.PermissionRequest
import dev.minios.ocremote.data.api.ProviderCatalogResponse
import dev.minios.ocremote.data.api.ProviderInfo
import dev.minios.ocremote.data.api.ProviderModel
import dev.minios.ocremote.data.api.ProvidersResponse
import dev.minios.ocremote.data.api.PromptPart
import dev.minios.ocremote.data.api.PtyInfo
import dev.minios.ocremote.data.api.PtySocket
import dev.minios.ocremote.data.api.ServerConfigResponse
import dev.minios.ocremote.data.api.ServerConnection
import dev.minios.ocremote.data.api.ServerPaths
import dev.minios.ocremote.domain.model.MessageWithParts
import dev.minios.ocremote.domain.model.Project
import dev.minios.ocremote.domain.model.ServerHealth
import dev.minios.ocremote.domain.model.Session
import dev.minios.ocremote.domain.model.SessionStatus
import dev.minios.ocremote.domain.model.ToolRef
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonPrimitive
import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Adapts [OpenCodeV2Api] to the V1-shaped DTOs the rest of the app already speaks.
 *
 * This is the seam that lets [dev.minios.ocremote.data.api.OpenCodeApi] route a call
 * to V2 without any UI or ViewModel changes. Where V2 has no equivalent (config
 * filters, forms) the adapter returns sensible empties and the call site degrades
 * gracefully. See docs/v2/MIGRATION.md phase 4 for the native replacements.
 */
@Singleton
class V2Gateway @Inject constructor(
    private val api: OpenCodeV2Api,
) {

    // ---- Server ------------------------------------------------------------

    suspend fun getHealth(conn: ServerConnection): ServerHealth =
        ServerHealth(healthy = true, version = api.getInfo(conn).version)

    suspend fun getServerPaths(conn: ServerConnection): ServerPaths {
        val info = api.getInfo(conn)
        return ServerPaths(home = info.paths.tmp)
    }

    // ---- Projects / agents / commands --------------------------------------

    suspend fun listProjects(conn: ServerConnection): List<Project> =
        api.listProjects(conn).map {
            Project(
                id = it.id,
                worktree = it.canonical,
                name = it.name,
                path = it.canonical,
                vcs = it.vcs,
                directory = it.canonical,
            )
        }

    suspend fun listAgents(conn: ServerConnection): List<AgentInfo> =
        api.listAgents(conn).map {
            AgentInfo(
                name = it.id.ifBlank { it.name },
                description = it.description,
                mode = it.mode,
                hidden = it.hidden,
                color = it.color,
            )
        }

    suspend fun listCommands(conn: ServerConnection): List<CommandInfo> =
        api.listCommands(conn).map {
            CommandInfo(name = it.name, description = it.description, source = "command")
        }

    // ---- Sessions ----------------------------------------------------------

    suspend fun listSessions(conn: ServerConnection, directory: String? = null): List<Session> =
        api.listSessions(conn, directory = directory, parentId = "null", limit = 200)
            .data.map { V2Mappers.sessionToDomain(it) }

    suspend fun listSessionStatuses(conn: ServerConnection, directory: String? = null): Map<String, SessionStatus> =
        api.listActiveSessions(conn, directory).mapValues { (_, active) ->
            when (active.type) {
                "running", "busy" -> SessionStatus.Busy
                else -> SessionStatus.Idle
            }
        }

    suspend fun getSession(conn: ServerConnection, sessionId: String, directory: String? = null): Session =
        V2Mappers.sessionToDomain(api.getSession(conn, sessionId, directory))

    suspend fun createSession(
        conn: ServerConnection,
        title: String? = null,
        parentId: String? = null,
        directory: String? = null,
    ): Session = V2Mappers.sessionToDomain(
        api.createSession(
            conn,
            V2CreateSessionRequest(title = title),
            directory,
        ),
    )

    suspend fun updateSession(conn: ServerConnection, sessionId: String, title: String): Session =
        V2Mappers.sessionToDomain(api.updateSession(conn, sessionId, V2UpdateSessionRequest(title = title)))

    suspend fun deleteSession(conn: ServerConnection, sessionId: String): Boolean =
        api.deleteSession(conn, sessionId)

    suspend fun abortSession(conn: ServerConnection, sessionId: String, directory: String? = null): Boolean =
        api.interrupt(conn, sessionId, directory)

    // ---- Messages ----------------------------------------------------------

    suspend fun listMessagesPage(
        conn: ServerConnection,
        sessionId: String,
        limit: Int? = null,
        before: String? = null,
        directory: String? = null,
    ): MessagePage {
        val page = api.listMessages(conn, sessionId, directory = directory, limit = limit, cursor = before)
        return MessagePage(
            messages = page.data.mapNotNull { V2Mappers.messageToDomain(it, sessionId) },
            nextCursor = page.cursor?.next,
        )
    }

    suspend fun getMessage(conn: ServerConnection, sessionId: String, messageId: String): MessageWithParts =
        V2Mappers.messageToDomain(api.getMessage(conn, sessionId, messageId), sessionId)
            ?: throw IOException("Unsupported V2 message type for $messageId")

    // ---- Prompt / control --------------------------------------------------

    suspend fun promptAsync(
        conn: ServerConnection,
        sessionId: String,
        messageId: String,
        parts: List<PromptPart>,
        model: ModelSelection? = null,
        agent: String? = null,
        variant: String? = null,
        directory: String? = null,
    ) {
        // V2 selects model/agent per session, so apply them before prompting.
        if (model != null) {
            runCatching {
                api.switchModel(
                    conn,
                    sessionId,
                    V2ModelRef(id = model.modelId, providerId = model.providerId, variant = variant),
                    directory,
                )
            }
        }
        if (!agent.isNullOrBlank()) {
            runCatching { api.switchAgent(conn, sessionId, agent, directory) }
        }

        val text = parts.filter { it.type == "text" }.mapNotNull { it.text }.joinToString("\n")
        val files = parts.filter { it.type == "file" }.map {
            V2PromptFile(uri = it.url ?: it.path.orEmpty(), name = it.filename)
        }
        api.prompt(
            conn,
            sessionId,
            V2PromptRequest(
                id = messageId.takeIf { it.startsWith("msg_") },
                text = text,
                files = files,
            ),
            directory,
        )
    }

    // ---- Permissions -------------------------------------------------------

    suspend fun listPendingPermissions(conn: ServerConnection, directory: String? = null): List<PermissionRequest> =
        api.listPermissions(conn, directory).map {
            PermissionRequest(
                id = it.id,
                sessionId = it.sessionId,
                permission = it.action,
                patterns = it.resources,
                metadata = it.metadata,
                always = it.save,
                tool = sourceToToolRef(it.source),
            )
        }

    suspend fun replyToPermission(
        conn: ServerConnection,
        requestId: String,
        reply: String,
        message: String? = null,
        directory: String? = null,
    ): Boolean {
        // V2 needs the owning session id; resolve it from the pending list.
        val sessionId = runCatching {
            api.listPermissions(conn, directory).firstOrNull { it.id == requestId }?.sessionId
        }.getOrNull() ?: return false
        return api.replyPermission(conn, sessionId, requestId, decision = reply, message = message, directory = directory)
    }

    // ---- Providers / models ------------------------------------------------

    private suspend fun mappedProviders(conn: ServerConnection): List<ProviderInfo> {
        val providers = api.listProviders(conn)
        val models = runCatching { api.listModels(conn) }.getOrDefault(emptyList())
        return providers.map { provider ->
            ProviderInfo(
                id = provider.id,
                name = provider.name,
                source = provider.activation,
                models = models.filter { it.providerId == provider.id }.associate { model ->
                    val modelId = model.modelId.ifBlank { model.id }
                    modelId to ProviderModel(
                        id = modelId,
                        providerId = provider.id,
                        name = model.name.ifBlank { modelId },
                        family = model.family,
                        status = model.status,
                    )
                },
            )
        }
    }

    suspend fun getProviders(conn: ServerConnection): ProvidersResponse {
        val defaultModel = runCatching { api.getDefaultModel(conn) }.getOrNull()
        val default = if (defaultModel != null) {
            mapOf(defaultModel.providerId to defaultModel.modelId.ifBlank { defaultModel.id })
        } else {
            emptyMap()
        }
        return ProvidersResponse(providers = mappedProviders(conn), default = default)
    }

    suspend fun listProviderCatalog(conn: ServerConnection): ProviderCatalogResponse {
        val providers = api.listProviders(conn)
        return ProviderCatalogResponse(
            all = mappedProviders(conn),
            default = emptyMap(),
            connected = providers.filter { it.activation != "disabled" }.map { it.id },
        )
    }

    /** V2 config is an entry array; native provider-filter support lands in phase 4. */
    suspend fun getConfig(conn: ServerConnection): ServerConfigResponse = ServerConfigResponse()

    // ---- Filesystem --------------------------------------------------------

    suspend fun listDirectory(conn: ServerConnection, path: String = "", directory: String? = null): List<FileNode> =
        api.listDirectory(conn, path, directory).map {
            FileNode(
                name = it.path.trimEnd('/').substringAfterLast('/').ifBlank { it.path },
                path = it.path,
                type = it.type,
                absolute = it.path,
            )
        }

    suspend fun findFiles(conn: ServerConnection, query: String, directory: String? = null): List<String> =
        api.findFiles(conn, query, directory).map { it.path }

    suspend fun readFile(conn: ServerConnection, path: String, directory: String? = null): FileContent {
        val bytes = api.readFile(conn, path, directory)
        return FileContent(type = "text", content = bytes.toString(Charsets.UTF_8), encoding = "utf-8")
    }

    // ---- PTY ---------------------------------------------------------------

    suspend fun createPty(
        conn: ServerConnection,
        title: String? = null,
        cwd: String? = null,
        directory: String? = null,
    ): PtyInfo {
        val pty = api.createPty(conn, title, cwd, directory)
        return PtyInfo(
            id = pty.id,
            title = pty.title,
            command = pty.command,
            args = pty.args,
            cwd = pty.cwd,
            status = pty.status,
            pid = pty.pid.toInt(),
        )
    }

    suspend fun updatePtySize(conn: ServerConnection, ptyId: String, cols: Int, rows: Int, directory: String? = null): Boolean =
        api.updatePtySize(conn, ptyId, cols, rows, directory)

    suspend fun removePty(conn: ServerConnection, ptyId: String): Boolean = api.removePty(conn, ptyId)

    suspend fun openPtySocket(conn: ServerConnection, ptyId: String, directory: String? = null): PtySocket =
        PtySocket(api.openPtySocket(conn, ptyId, directory))

    // ---- helpers -----------------------------------------------------------

    private fun sourceToToolRef(source: JsonElement?): ToolRef? {
        val obj = source as? JsonObject ?: return null
        val messageId = obj["messageID"]?.jsonPrimitive?.contentOrNull ?: return null
        return ToolRef(messageId = messageId, callId = obj["id"]?.jsonPrimitive?.contentOrNull.orEmpty())
    }
}
