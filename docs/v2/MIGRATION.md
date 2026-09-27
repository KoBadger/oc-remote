# OC Remote → OpenCode V2 migration

Status: **in progress** (transport layer started). Target: OpenCode **2.x** server API.

## Why this is needed

OC Remote v1.10.0 speaks the **OpenCode V1 server API**. OpenCode V2 changes the
server API on purpose (see the official migration guide: *"Integrations that call
the V1 server API must migrate to the V2 API"*). Concretely:

| Area | V1 (current OC Remote) | V2 |
|---|---|---|
| Server info | `GET /global/health` | `GET /api/info` |
| REST prefix | none (`/session`, `/agent`, …) | `/api/...` everywhere |
| Session list | `GET /session?roots=true` → array | `GET /api/session` → `{data, cursor}`, query `limit/order/search/parentID/directory/cursor` |
| Messages | `GET /session/{id}/message` → `[{info, parts}]` | `GET /api/session/{id}/message` → `{data:[Session.Message.Info], cursor}` with **parts inline** |
| Prompt | `POST /session/{id}/prompt_async` | `POST /api/session/{id}/prompt` → `{data: Session.Inbox.User}` |
| Abort | `POST /session/{id}/abort` | `POST /api/session/{id}/interrupt` |
| Questions | `GET /question`, `POST /question/{id}/reply` | replaced by **Forms**: `GET /api/form`, `POST /api/session/{sid}/form/{id}/reply` |
| Permissions | `POST /permission/{id}/reply` | `POST /api/session/{sid}/permission/{id}/reply` (`{decision,message?}`) |
| Events | `GET /global/event`, names `session.next.*`, `message.part.*` | `GET /api/event`, names `session.*`, `message.*`, `permission.*`, `form.*` |
| Models/providers | `GET /config/providers`, `GET /provider` | `GET /api/model`, `GET /api/provider`, `GET /api/model/default` |
| Provider auth | `PUT/DELETE /auth/{id}`, `POST /provider/{id}/oauth/*` | `POST /api/integration/{id}/connect/{key,oauth,command}`, `PATCH/DELETE /api/credential/{id}` |
| Files | `/find`, `/find/file`, `/file`, `/file/content` | `/api/fs/find`, `/api/fs/list`, `GET /api/fs/read/*` |
| Config | `GET/PATCH /config`, `/global/config` | `GET /api/config` (array), `PATCH /api/experimental/config` |
| PTY | `/pty` (+ WS `/pty/{id}/connect`) | `/api/pty` (+ `GET /api/pty/{id}/connect`, `POST /api/pty/{id}/connect-token`) |

Authored references (kept in `docs/v2/`):

- `docs/v2/OPERATIONS.md` — every operation (method, path, operationId).
- `docs/v2/SHAPES.md` — exact `Session.*`, `Session.Message.*`, `Form.*`, request bodies.
- `docs/v2/RESPONSES.md` — response envelope (`{data}` vs `{location,data}` vs array) per endpoint.
- `docs/v2/SECURITY.md` — no OpenAPI security scheme; auth is HTTP-layer.

Vendored snapshots (kept in `docs/v2/reference/`, read-only):

- `reference/openapi.json` — the machine contract (136 operations, 245 schemas).
- `reference/opencode-v2-types.gen.ts` — official generated V2 SDK types.
- `reference/openchamber-client.ts` + `reference/openchamber-hits.md` — a real, shipping
  V2 client (OpenChamber) used as a behavioural cross-check.

## Strategy

Keep the **domain + UI + EventReducer** surface and swap the transport behind it.
Do not rewrite the whole app. Concretely:

1. Add a V2 transport layer (`data/api/v2/`), independent of the existing V1 `OpenCodeApi`.
2. Detect server version at connect time and route calls to V1 or V2.
   - Probe `GET /api/info`. If it returns JSON with `version`, the server is V2.
   - Otherwise fall back to `GET /global/health` (V1).
   - Cache the result on the `ServerConnection`.
3. Translate V2 wire data into the **existing domain models** (`Session`, `Message`,
   `Part`, `SseEvent`) so `EventReducer` and the Compose UI keep working.
   - `V2Mappers` handles `Session.Info → Session` and `Session.Message.Info → MessageWithParts`.
   - `V2EventParser` maps `session.*` event names onto the existing `SseEvent` sealed types
     (`session.text.delta` → `NextTextDelta`, etc.).
4. Only where V2 has no equivalent (e.g. Forms vs Questions, `api/integration` auth) add
   new UI paths; leave the V1 paths intact for backward compatibility.

### Auth

- V2 declares **no** OpenAPI security schemes, but a server with a password enforces auth at
  the HTTP layer. Two supported client modes:
  - **Basic auth** with `username` (default `opencode`) + `password` (`OPENCODE_SERVER_PASSWORD`).
    This is what the existing `ServerConnection.authHeader` already produces — reuse it.
  - **Paired session cookie** from `opencode pair` / `opencode service set password`
    (browser link). Store the cookie and send it as `Cookie`, or use the returned bearer token.
- `401` must be surfaced distinctly so the connect flow can prompt for a password.

### Pagination

V2 list endpoints are cursor-based: responses carry `cursor.previous` / `cursor.next`.
`GET /api/session` and `/api/session/{id}/message` accept `cursor` + `limit`. The app's
existing "newest N then background-load older" history logic maps onto `cursor.next`.

## Phased plan

- [x] **Phase 0 — Recon.** Clone repo, capture V2 OpenAPI + official generated types,
  enumerate operations/schemas/envelopes/events, choose a reference client (OpenChamber).
- [x] **Phase 1 — Transport (this change).** ✅ compiles; 7 new unit tests pass.
  - `V2Dto.kt` — V2 wire DTOs.
  - `OpenCodeV2Api.kt` — core endpoints (info, projects, agents, models, providers,
    sessions CRUD, messages, prompt, interrupt, permissions, forms, commands, files, PTY, config).
  - `V2EventParser.kt` — `GET /api/event` frame parsing → existing `SseEvent`.
  - `V2Mappers.kt` — V2 → domain model translation.
  - `ServerApiVersion` probe.
- [ ] **Phase 2 — Wire-up + DI.** Add `@Provides` for the V2 client; route call sites in
  `OpenCodeApi`/repositories through a `Transport` facade chosen by `ServerApiVersion`.
- [ ] **Phase 3 — Events.** Switch `SseClient` to `/api/event` on V2, feed the same reducer;
  handle `session.*` names and inline `message.part.*` deltas.
- [ ] **Phase 4 — Feature parity.** Forms UI (replaces Questions), integration/credential
  provider auth, `api/fs/*` file browser, `api/pty` terminal, `api/config` settings.
- [ ] **Phase 5 — Phone runtime.** Run OpenCode V2 on the phone (`opencode2` package from
  `guysoft/opencode-termux` / native bionic from `Hope2333/opencode-termux`) with
  `opencode serve --hostname 0.0.0.0 --port 4096`, or on the desktop; connect over LAN/Tailscale.
- [ ] **Phase 6 — Tests + release.** Golden-JSON fixtures from a running V2 server; instrumented
  tests with MockWebServer; version-dispatch tests.

## Build notes

- Project pins Gradle **8.6** + AGP **8.4** + JDK target **17**. Gradle 8.6 cannot run on
  JDK 22+, so build with **JDK 17 or 21** (Android Studio → Settings → Build → Gradle →
  Gradle JDK → Download JDK 17).
- The repo ships a Unix `gradlew` but **no `gradlew.bat`**. On Windows either add the batch
  file or invoke the wrapper jar directly. Verified working command (PowerShell, repo root):

  ```powershell
  $env:JAVA_HOME = 'C:\path\to\jdk-17'
  $env:ANDROID_HOME = "$env:LOCALAPPDATA\Android\Sdk"
  $env:ANDROID_SDK_ROOT = $env:ANDROID_HOME
  & "$env:JAVA_HOME\bin\java.exe" -cp "gradle\wrapper\gradle-wrapper.jar" `
      org.gradle.wrapper.GradleWrapperMain --console=plain :app:testDebugUnitTest
  ```

  `:app:assembleDebug` produces `app/build/outputs/apk/debug/app-debug.apk`.
- `ignoreUnknownKeys` is already enabled in `NetworkModule`, so V2 payloads with extra fields
  deserialize safely during the transition.

## Risks / open questions

- **Message model mismatch** is the largest change: V2 assistant messages store an inline
  `content: [text|reasoning|tool]` array rather than separate `message` + `part` events. The
  mapper is best-effort; long-term the reducer and chat renderer should consume V2 parts natively.
- **Task/Subtask and Snapshot/Patch parts** have no obvious V2 equivalents; confirm against 2.0.x
  (`Session.Message.Info` union) before porting those UI cards.
- **Sharing** has no `share/unshare` operation in the V2 OpenAPI; re-check whether it moved to
  Console.
- V2 is still moving (OpenChamber pins 2.0.x and keeps workarounds). Pin a known-good server
  version for testing.
