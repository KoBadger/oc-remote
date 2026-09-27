=== securitySchemes ===
{}

=== top-level security ===
[]

=== schema V2EventEncoded ===
{
  "type": "string",
  "contentMediaType": "application/json"
}

=== schema ServerInfo ===
{
  "type": "object",
  "properties": {
    "version": {
      "type": "string"
    },
    "pid": {
      "type": "integer",
      "minimum": 0
    },
    "urls": {
      "type": "array",
      "items": {
        "type": "string"
      }
    },
    "paths": {
      "type": "object",
      "properties": {
        "tmp": {
          "type": "string"
        }
      },
      "required": [
        "tmp"
      ],
      "additionalProperties": false
    }
  },
  "required": [
    "version",
    "pid",
    "urls",
    "paths"
  ],
  "additionalProperties": false
}

=== schema Permission.Request ===
{
  "type": "object",
  "properties": {
    "id": {
      "type": "string",
      "pattern": "^per"
    },
    "sessionID": {
      "type": "string",
      "pattern": "^ses"
    },
    "action": {
      "type": "string"
    },
    "resources": {
      "type": "array",
      "items": {
        "type": "string"
      }
    },
    "save": {
      "type": "array",
      "items": {
        "type": "string"
      }
    },
    "metadata": {
      "type": "object"
    },
    "source": {
      "$ref": "#/components/schemas/Permission.Source"
    },
    "message": {
      "type": "string"
    }
  },
  "required": [
    "id",
    "sessionID",
    "action",
    "resources"
  ],
  "additionalProperties": false
}

=== schema Permission.Reply ===
{
  "type": "string",
  "enum": [
    "once",
    "always",
    "reject"
  ]
}

=== schema Form.Reply ===
{
  "type": "object",
  "properties": {
    "answer": {
      "$ref": "#/components/schemas/Form.Answer"
    }
  },
  "required": [
    "answer"
  ],
  "additionalProperties": false
}

=== schema Form.Field ===
{
  "anyOf": [
    {
      "$ref": "#/components/schemas/Form.StringField"
    },
    {
      "$ref": "#/components/schemas/Form.NumberField"
    },
    {
      "$ref": "#/components/schemas/Form.IntegerField"
    },
    {
      "$ref": "#/components/schemas/Form.BooleanField"
    },
    {
      "$ref": "#/components/schemas/Form.MultiselectField"
    },
    {
      "$ref": "#/components/schemas/Form.ExternalField"
    }
  ]
}

=== GET /api/session security/params ===
{"security": [], "parameters": [{"name": "limit", "in": "query", "schema": {"anyOf": [{"type": "string"}, {"type": "null"}], "description": "Maximum number of sessions to return. Defaults to the newest 50 sessions."}, "required": false}, {"name": "order", "in": "query", "schema": {"anyOf": [{"type": "string", "enum": ["asc", "desc"]}, {"type": "null"}], "description": "Session order for the first page. Use desc for newest first or asc for oldest first."}, "required": false}, {"name": "search", "in": "query", "schema": {"anyOf": [{"type": "string"}, {"type": "null"}]}, "required": false}, {"name": "parentID", "in": "query", "schema": {"anyOf": [{"anyOf": [{"type": "string", "pattern": "^ses"}, {"type": "string", "enum": ["null"]}], "description": "Filter by parent session. Use null to return only root sessions."}, {"type": "null"}]}, "required": false}, {"name": "directory", "in": "query", "schema": {"anyOf": [{"type": "string"}, {"type": "null"}]}, "required": false}, {"name": "project", "in": "query", "schema": {"anyOf": [{"type": "string"}, {"type": "null"}]}, "required": false}, {"name": "subpath", "in": "query", "schema": {"anyOf": [{"type": "string"}, {"type": "null"}]}, "required": false}, {"name": "cursor", "in": "query", "schema": {"anyOf": [{"type": "string", "description": "Opaque pagination cursor returned as cursor.previous or cursor.next in the previous response."}, {"type": "null"}]}, "required": false}], "responses": ["200", "400", "401"]}

=== GET /api/event security/params ===
{"security": [], "parameters": [], "responses": ["200", "400", "401"]}

=== GET /api/info security/params ===
{"security": [], "parameters": [], "responses": ["200", "400", "401"]}