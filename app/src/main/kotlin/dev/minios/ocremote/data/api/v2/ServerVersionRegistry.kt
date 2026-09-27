package dev.minios.ocremote.data.api.v2

import dev.minios.ocremote.data.api.ServerConnection
import java.util.concurrent.ConcurrentHashMap
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Caches the detected [ServerApiVersion] per server base URL so the transport can
 * route each call to the V1 or V2 client. Detection is lazy and retried while
 * the result is [ServerApiVersion.UNKNOWN].
 */
@Singleton
class ServerVersionRegistry @Inject constructor(
    private val v2Api: OpenCodeV2Api,
) {
    private val cache = ConcurrentHashMap<String, ServerApiVersion>()

    suspend fun version(conn: ServerConnection, force: Boolean = false): ServerApiVersion {
        if (!force) {
            cache[conn.baseUrl]?.takeIf { it != ServerApiVersion.UNKNOWN }?.let { return it }
        }
        val detected = v2Api.detectVersion(conn)
        if (detected != ServerApiVersion.UNKNOWN) {
            cache[conn.baseUrl] = detected
        }
        return detected
    }

    fun cached(conn: ServerConnection): ServerApiVersion? =
        cache[conn.baseUrl]?.takeIf { it != ServerApiVersion.UNKNOWN }

    fun set(conn: ServerConnection, version: ServerApiVersion) {
        cache[conn.baseUrl] = version
    }

    fun invalidate(conn: ServerConnection) {
        cache.remove(conn.baseUrl)
    }
}
