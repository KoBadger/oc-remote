
GET /api/info  (200=['200', '400', '401'])
  {"media": "application/json", "schema": {"$ref": "#/components/schemas/ServerInfo"}}

GET /api/project  (200=['200', '400', '401'])
  {"media": "application/json", "schema": {"type": "array", "items": {"$ref": "#/components/schemas/Project"}}}

GET /api/agent  (200=['200', '400', '401'])
  {"media": "application/json", "schema": {"type": "object", "properties": {"location": {"$ref": "#/components/schemas/Location.PublicRef"}, "data": {"type": "array", "items": {"$ref": "#/components/schemas/Agent.Info"}}}, "required": ["location", "data"], "additionalProperties": false}}

GET /api/command  (200=['200', '400', '401'])
  {"media": "application/json", "schema": {"type": "object", "properties": {"location": {"$ref": "#/components/schemas/Location.PublicRef"}, "data": {"type": "array", "items": {"$ref": "#/components/schemas/Command.Info"}}}, "required": ["location", "data"], "additionalProperties": false}}

GET /api/model  (200=['200', '400', '401', '503'])
  {"media": "application/json", "schema": {"type": "object", "properties": {"location": {"$ref": "#/components/schemas/Location.PublicRef"}, "data": {"type": "array", "items": {"$ref": "#/components/schemas/Model.Info"}}}, "required": ["location", "data"], "additionalProperties": false}}

GET /api/model/default  (200=['200', '400', '401', '503'])
  {"media": "application/json", "schema": {"type": "object", "properties": {"location": {"$ref": "#/components/schemas/Location.PublicRef"}, "data": {"anyOf": [{"$ref": "#/components/schemas/Model.Info"}, {"type": "null"}]}}, "required": ["location", "data"], "additionalProperties": false}}

GET /api/provider  (200=['200', '400', '401', '503'])
  {"media": "application/json", "schema": {"type": "object", "properties": {"location": {"$ref": "#/components/schemas/Location.PublicRef"}, "data": {"type": "array", "items": {"$ref": "#/components/schemas/Provider.Info"}}}, "required": ["location", "data"], "additionalProperties": false}}

GET /api/provider/{providerID}  (200=['200', '400', '401', '404', '503'])
  {"media": "application/json", "schema": {"type": "object", "properties": {"location": {"$ref": "#/components/schemas/Location.PublicRef"}, "data": {"$ref": "#/components/schemas/Provider.Info"}}, "required": ["location", "data"], "additionalProperties": false}}

GET /api/permission/request  (200=['200', '400', '401'])
  {"media": "application/json", "schema": {"type": "object", "properties": {"location": {"$ref": "#/components/schemas/Location.PublicRef"}, "data": {"type": "array", "items": {"$ref": "#/components/schemas/Permission.Request"}}}, "required": ["location", "data"], "additionalProperties": false}}

GET /api/form  (200=['200', '400', '401'])
  {"media": "application/json", "schema": {"type": "object", "properties": {"location": {"$ref": "#/components/schemas/Location.PublicRef"}, "data": {"type": "array", "items": {"$ref": "#/components/schemas/Form.Info"}}}, "required": ["location", "data"], "additionalProperties": false}}

GET /api/mcp  (200=['200', '400', '401'])
  {"media": "application/json", "schema": {"type": "object", "properties": {"location": {"$ref": "#/components/schemas/Location.PublicRef"}, "data": {"type": "array", "items": {"$ref": "#/components/schemas/Mcp.Server"}}}, "required": ["location", "data"], "additionalProperties": false}}

GET /api/session/active  (200=['200', '400', '401'])
  {"media": "application/json", "schema": {"type": "object", "properties": {"data": {"type": "object", "patternProperties": {"^ses": {"$ref": "#/components/schemas/SessionActive"}}}}, "required": ["data"], "additionalProperties": false}}

GET /api/pty  (200=['200', '400', '401'])
  {"media": "application/json", "schema": {"type": "object", "properties": {"location": {"$ref": "#/components/schemas/Location.PublicRef"}, "data": {"type": "array", "items": {"$ref": "#/components/schemas/Pty"}}}, "required": ["location", "data"], "additionalProperties": false}}

GET /api/fs/list  (200=['200', '400', '401'])
  {"media": "application/json", "schema": {"type": "object", "properties": {"location": {"$ref": "#/components/schemas/Location.PublicRef"}, "data": {"type": "array", "items": {"$ref": "#/components/schemas/FileSystem.Entry"}}}, "required": ["location", "data"], "additionalProperties": false}}

GET /api/fs/find  (200=['200', '400', '401'])
  {"media": "application/json", "schema": {"type": "object", "properties": {"location": {"$ref": "#/components/schemas/Location.PublicRef"}, "data": {"type": "array", "items": {"$ref": "#/components/schemas/FileSystem.Entry"}}}, "required": ["location", "data"], "additionalProperties": false}}

GET /api/fs/read/*  (200=['200', '400', '401', '404'])
  {"media": "application/octet-stream", "schema": {"type": "string", "format": "binary"}}

GET /api/config  (200=['200', '400', '401'])
  {"media": "application/json", "schema": {"type": "array", "items": {"$ref": "#/components/schemas/Config.Entry"}}}

GET /api/location  (200=['200', '400', '401'])
  {"media": "application/json", "schema": {"$ref": "#/components/schemas/Location.PublicInfo"}}

GET /api/vcs/status  (200=['200', '400', '401'])
  {"media": "application/json", "schema": {"type": "object", "properties": {"location": {"$ref": "#/components/schemas/Location.PublicRef"}, "data": {"type": "array", "items": {"$ref": "#/components/schemas/Vcs.FileStatus"}}}, "required": ["location", "data"], "additionalProperties": false}}

GET /api/vcs/diff  (200=['200', '400', '401', '503'])
  {"media": "application/json", "schema": {"type": "object", "properties": {"location": {"$ref": "#/components/schemas/Location.PublicRef"}, "data": {"type": "array", "items": {"$ref": "#/components/schemas/FileDiff.Info"}}}, "required": ["location", "data"], "additionalProperties": false}}

POST /api/session/{sessionID}/prompt  (200=['200', '400', '401', '404', '409'])
  {"media": "application/json", "schema": {"type": "object", "properties": {"data": {"$ref": "#/components/schemas/Session.Inbox.User"}}, "required": ["data"], "additionalProperties": false}}

POST /api/session  (200=['200', '400', '401'])
  {"media": "application/json", "schema": {"type": "object", "properties": {"data": {"$ref": "#/components/schemas/Session.Info"}}, "required": ["data"], "additionalProperties": false}}

GET /api/session/{sessionID}  (200=['200', '400', '401', '404'])
  {"media": "application/json", "schema": {"type": "object", "properties": {"data": {"$ref": "#/components/schemas/Session.Info"}}, "required": ["data"], "additionalProperties": false}}

GET /api/session/{sessionID}/diff  (200=['200', '400', '401', '404', '500'])
  {"media": "application/json", "schema": {"type": "object", "properties": {"data": {"type": "array", "items": {"$ref": "#/components/schemas/FileDiff.Info"}}}, "required": ["data"], "additionalProperties": false}}

POST /api/session/{sessionID}/interrupt  (200=['200', '400', '401', '404'])
  {"media": "application/json", "schema": {"$ref": "#/components/schemas/SessionInterruptResponse"}}

GET /api/session/{sessionID}/context  (200=['200', '400', '401', '404', '500'])
  {"media": "application/json", "schema": {"type": "object", "properties": {"data": {"type": "array", "items": {"$ref": "#/components/schemas/Session.Message.Info"}}}, "required": ["data"], "additionalProperties": false}}

GET /api/event  (200=['200', '400', '401'])
  {"media": "text/event-stream", "schema": {"type": "object", "properties": {"id": {"anyOf": [{"type": "string"}, {"type": "null"}]}, "event": {"type": "string"}, "data": {"$ref": "#/components/schemas/V2EventEncoded"}}, "required": ["id", "event", "data"], "additionalProperties": false}}