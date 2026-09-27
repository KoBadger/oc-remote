11: * OpenChamber-owned server routes (`/api/fs/*`, `/api/opencode/*`) also live
12: * here when they are part of the same directory/session workflows.
18: SessionDiffInput,
24: SessionInboxDelivery,
25: SessionRevert,
28: import type { FilesAPI } from "../api/types"
31: import { FilesystemError, parseFilesystemErrorReason } from "@/lib/api/files-errors"
43: import { activeSessionSnapshotSchema, hostSessionStatusSnapshotSchema, type HostSessionStatusSnapshot } from "./session-status"
59: type Session,
60: type SessionStatus,
65: import { mergeConfigDocuments, projectAgent, projectMessages, projectProject, projectSession, projectVcs } from "./projection"
74: const DEFAULT_SESSION_PAGE_LIMIT = 100
91: ["SessionNotFoundError", 404],
105: ["SessionBusyError", 409],
116: /** The error class OpenCode named in a tagged body (`SessionNotFoundError`, `UnknownError`, ...). */
181: * whatever the session is doing; a name that cannot be attached falls back to
211: const ensureAbsoluteBaseUrl = (candidate: string): string => {
235: const resolveRuntimeBaseUrl = (): string | null => {
265: * "loading sessions". Long-lived streams (prompts, the event SSE, session logs)
270: const isEventStreamUrl = (url: URL): boolean => url.pathname.endsWith("/event") || url.pathname.endsWith("/log")
276: baseUrl: string
283: * The generated client joins its `/api/...` route paths onto the base URL's
287: const toOpencodeClientRoot = (baseUrl: string): string => baseUrl.replace(/\/api\/*$/, "") || "/"
292: baseUrl: toOpencodeClientRoot(config.baseUrl),
293: headers: config.directory ? { [OPENCODE_DIRECTORY_HEADER]: encodeURIComponent(config.directory) } : undefined,
298: return runtimeFetch(input, init)
341: const response = await runtimeFetch(input, { ...init, signal })
399: export type SessionPage = {
400: sessions: Session[]
410: const sessionPageSpacesSchema = z.object({ spaces: z.array(spaceMarkSchema).optional() })
412: export type SessionListOptions = {
414: /** No directory filter at all: every session the server knows. */
458: // /api/fs/home parsing boundary. Older servers answer without chatsRoot;
499: private baseUrl: string
511: constructor(baseUrl: string = DEFAULT_BASE_URL) {
512: const runtimeBase = resolveRuntimeBaseUrl()
513: const requestedBaseUrl = runtimeBase || baseUrl
514: this.baseUrl = ensureAbsoluteBaseUrl(requestedBaseUrl)
515: this.client = createRuntimeOpencodeClient({ baseUrl: this.baseUrl })
530: getBaseUrl(): string {
531: return this.baseUrl
534: reconnectToRuntimeBaseUrl(): void {
535: const runtimeBase = resolveRuntimeBaseUrl()
536: const nextBaseUrl = ensureAbsoluteBaseUrl(runtimeBase || DEFAULT_BASE_URL)
539: this.baseUrl = nextBaseUrl
540: this.client = createRuntimeOpencodeClient({ baseUrl: this.baseUrl })
562: const scoped = createRuntimeOpencodeClient({ baseUrl: this.baseUrl, directory: normalized })
716: const sessions = await this.listSessions()
717: sessions.forEach((session) => addCandidate(session.directory))
719: console.debug("Failed to inspect sessions for system info:", error)
759: * The probe is OpenChamber's own `/api/fs/directory-stat`, which asks the
769: const response = await runtimeFetch("/api/fs/directory-stat", { query: { path: normalized } })
780: // Sessions
783: /** One page of sessions. Without `global`, scoped to the given or current directory. */
784: async listSessionsPage(options: SessionListOptions = {}): Promise<SessionPage> {
787: const response = await call("session.list", () =>
788: client.session.list({
790: limit: options.limit ?? DEFAULT_SESSION_PAGE_LIMIT,
797: const spaces = options.global ? sessionPageSpacesSchema.safeParse(response).data?.spaces : undefined
799: sessions: response.data.map(projectSession),
805: /** First page of sessions for the current directory. */
806: async listSessions(directory?: string | null): Promise<Session[]> {
807: const page = await this.listSessionsPage({ directory })
808: return page.sessions
811: async createSession(
814: ): Promise<Session> {
816: const info = await call("session.create", () =>
817: this.clientFor(directory).session.create({
822: // session on; the first send puts the session on Auto through the
829: return projectSession(info)
832: async getSession(id: string, directory?: string | null): Promise<Session> {
833: const info = await call("session.get", () => this.clientFor(directory).session.get({ sessionID: id }))
834: return projectSession(info)
837: async deleteSession(id: string, directory?: string | null): Promise<boolean> {
838: await call("session.remove", () => this.clientFor(directory).session.remove({ sessionID: id }))
843: * Renames the session. OpenCode 2.0.8 folded `session.rename` into
844: * `session.update`, where an empty title asks the server to regenerate one.
846: async renameSession(id: string, title: string, directory?: string | null): Promise<void> {
847: await call("session.update", () => this.clientFor(directory).session.update({ sessionID: id, title }))
850: async moveSession(id: string, toDirectory: string, options?: { delivery?: SessionInboxDelivery }): Promise<void> {
852: await call("session.move", () =>
853: this.client.session.move({ sessionID: id, directory, delivery: options?.delivery }),
857: async switchSessionModel(id: string, model: ModelRef, directory?: string | null): Promise<void> {
858: await call("session.switchModel", () => this.clientFor(directory).session.switchModel({ sessionID: id, model }))
861: async switchSessionAgent(id: string, agent: string, directory?: string | null): Promise<void> {
862: await call("session.switchAgent", () => this.clientFor(directory).session.switchAgent({ sessionID: id, agent }))
866: * One page of a session's messages, newest first by default. `cursor` comes
874: async getSessionMessages(
881: sessionID: id,
895: async getSessionMessage(id: string, messageID: string, directory?: string | null): Promise<{ info: Message; parts: Part[] }> {
896: const info = await call("session.message.get", () => this.clientFor(directory).session.message.get({ sessionID: id, messageID }))
1040: * Puts a session on the requested model/agent before a prompt. v2 selects
1041: * both per session, not per prompt; the choice persists until switched.
1044: sessionID: string,
1051: await this.switchSessionModel(sessionID, selection.model, directory)
1055: await this.switchSessionAgent(sessionID, selection.agent, directory)
1071: /** Switch the session to this model before sending; omit when unchanged. */
1073: /** Switch the session to this agent before sending; omit when unchanged. */
1084: delivery?: SessionInboxDelivery
1108: await call("session.synthetic", () =>
1109: this.clientFor(params.directory).session.synthetic({
1110: sessionID: params.id,
1121: return call("session.prompt", () =>
1122: this.clientFor(params.directory).session.prompt({
1123: sessionID: params.id,
1168: * Runs a slash command in the session. The server assigns the message id.
1182: delivery?: SessionInboxDelivery
1191: await call("session.synthetic", () =>
1192: this.clientFor(params.directory).session.synthetic({
1193: sessionID: params.id,
1203: await call("session.command", () =>
1204: this.clientFor(params.directory).session.command({
1205: sessionID: params.id,
1215: async abortSession(id: string, directory?: string | null): Promise<boolean> {
1216: const result = await call("session.interrupt", () => this.clientFor(directory).session.interrupt({ sessionID: id }))
1220: /** Runs a shell command inside the session transcript. Returns the shell message id. */
1221: async shellSession(params: {
1223: sessionId: string
1230: await call("session.shell", () =>
1231: this.clientFor(params.directory).session.shell({ sessionID: params.sessionId, id, command: params.command }),
1237: async stageRevert(sessionId: string, messageId: string, options?: { files?: boolean; directory?: string | null }): Promise<SessionRevert> {
1238: return call("session.revert.stage", () =>
1239: this.clientFor(options?.directory).session.revert.stage({
1240: sessionID: sessionId,
1247: async commitRevert(sessionId: string, directory?: string | null): Promise<void> {
1248: await call("session.revert.commit", () => this.clientFor(directory).session.revert.commit({ sessionID: sessionId }))
1251: async clearRevert(sessionId: string, directory?: string | null): Promise<void> {
1252: await call("session.revert.clear", () => this.clientFor(directory).session.revert.clear({ sessionID: sessionId }))
1256: async compactSession(sessionId: string, directory?: string | null): Promise<void> {
1257: await call("session.compact", () => this.clientFor(directory).session.compact({ sessionID: sessionId }))
1261: * Forks the session. `before` copies the transcript up to but excluding that
1265: async forkSession(sessionId: string, options?: { before?: string; directory?: string | null }): Promise<Session> {
1266: const info = await call("session.fork", () =>
1267: this.clientFor(options?.directory).session.fork({ sessionID: sessionId, before: options?.before }),
1269: return projectSession(info)
1273: * Sessions with a running agent loop, or `null` when the fetch failed.
1276: * session is idle, so a candidate missing from it is authoritatively idle.
1281: * because the host's snapshot never covers a space's sessions, and an empty
1284: async getActiveSessionStatuses(directory?: string | null): Promise<Record<string, SessionStatus> | null> {
1287: const active = activeSessionSnapshotSchema.parse(await call("session.active", () => client.session.active()))
1288: const statuses: Record<string, SessionStatus> = {}
1289: for (const sessionID of Object.keys(active)) statuses[sessionID] = { type: "busy" }
1299: * instance, unlike `/session/status?directory=`. `null` means the fetch
1302: async getHostSessionStatusSnapshot(): Promise<HostSessionStatusSnapshot | null> {
1304: const response = await runtimeFetch('/api/sessions/status', {
1306: headers: { Accept: 'application/json' },
1311: const parsed = hostSessionStatusSnapshotSchema.safeParse(await response.json().catch(() => null));
1319: * Get session activity from web server's in-memory tracking.
1323: async getWebServerSessionActivity(): Promise<Record<string, { type: string }> | null> {
1325: const response = await runtimeFetch("/api/session-activity", {
1327: headers: {
1347: /** Transient text generated from the session's context; never enters history. */
1348: async generateSessionText(sessionId: string, prompt: string, directory?: string | null): Promise<string> {
1349: const result = await call("session.generate", () =>
1350: this.clientFor(directory).session.generate({ sessionID: sessionId, prompt }),
1361: async getSessionTurnDiff(
1362: sessionId: string,
1367: const input: SessionDiffInput = { sessionID: sessionId, from, to, context }
1368: const result = await call("session.diff", () => this.clientFor(options?.directory).session.diff(input))
1385: sessionID: string,
1392: sessionID,
1403: * permission request for a session.
1410: sessionID: string,
1425: sessionID,
1442: * Fetch a pending permission request owned by a session. A 404 is the
1446: async fetchPermission(sessionID: string, requestID: string, directory?: string | null): Promise<FetchPermissionResult> {
1448: const permission = await call("permission.get", () => this.clientFor(directory).permission.get({ sessionID, requestID }))
1478: async replyToForm(sessionID: string, formID: string, answer: FormAnswer, directory?: string | null): Promise<boolean> {
1479: await call("session.form.reply", () => this.clientFor(directory).session.form.reply({ sessionID, formID, answer }))
1483: async cancelForm(sessionID: string, formID: string, directory?: string | null): Promise<boolean> {
1484: await call("session.form.cancel", () => this.clientFor(directory).session.form.cancel({ sessionID, formID }))
1684: const normalizedBase = this.baseUrl.endsWith("/") ? this.baseUrl.replace(/\/+$/, "") : this.baseUrl
1686: normalizedBase === "/api" || normalizedBase.endsWith("/api") ? "/api/opencode/health" : `${normalizedBase}/opencode/health`
1687: markStartupTrace("opencodeClient.checkHealth:url", { baseUrl: this.baseUrl, healthUrl })
1691: response = await runtimeFetch(healthUrl, { signal: timeout.signal }).finally(timeout.cleanup)
1731: const response = await runtimeFetch(`${this.baseUrl}/opencode/directory`, {
1733: headers: {
1753: const response = await runtimeFetch(`${this.baseUrl}/fs/mkdir`, {
1755: headers: {
1771: const response = await runtimeFetch(`${this.baseUrl}/fs/clone`, {
1773: headers: {
1834: const response = await runtimeFetch(`${this.baseUrl}/fs/list${query ? `?${query}` : ""}`)
1917: // /api/fs/home, not from the local Electron global.
1927: const response = await runtimeFetch(`${this.baseUrl}/fs/home`, {
1929: headers: {
1955: const response = await runtimeFetch(`${this.baseUrl}/fs/home`, {
1957: headers: {
1973: const url = `${this.baseUrl}/opencode/directory`
1976: const response = await runtimeFetch(url, {
1978: headers: {