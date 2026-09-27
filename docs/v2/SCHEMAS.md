
## V2EventEncoded

```json
{
  "type": "string",
  "contentMediaType": "application/json"
}
```

## Session.Info

```json
{
  "type": "object",
  "properties": {
    "id": {
      "type": "string",
      "pattern": "^ses"
    },
    "parentID": {
      "type": "string",
      "pattern": "^ses"
    },
    "fork": {
      "type": "object",
      "properties": {
        "sessionID": {
          "type": "string",
          "pattern": "^ses"
        },
        "boundary": {
          "$ref": "#/components/schemas/Session.ForkBoundary"
        }
      },
      "required": [
        "sessionID",
        "boundary"
      ],
      "additionalProperties": false
    },
    "projectID": {
      "type": "string"
    },
    "agent": {
      "type": "string"
    },
    "model": {
      "$ref": "#/components/schemas/Model.Ref"
    },
    "cost": {
      "$ref": "#/components/schemas/Money.USD"
    },
    "tokens": {
      "$ref": "#/components/schemas/TokenUsage.Info"
    },
    "outcome": {
      "type": "string",
      "enum": [
        "succeeded",
        "failed",
        "interrupted"
      ]
    },
    "time": {
      "type": "object",
      "properties": {
        "created": {
          "type": "number"
        },
        "updated": {
          "type": "number"
        },
        "idle": {
          "type": "number"
        },
        "viewed": {
          "type": "number"
        },
        "archived": {
          "type": "number"
        }
      },
      "required": [
        "created",
        "updated"
      ],
      "additionalProperties": false
    },
    "title": {
      "type": "string"
    },
    "subpath": {
      "type": "string"
    },
    "metadata": {
      "$ref": "#/components/schemas/Session.Metadata"
    },
    "permissions": {
      "$ref": "#/components/schemas/Permission.Ruleset"
    },
    "revert": {
      "$ref": "#/components/schemas/Session.Revert"
    },
    "location": {
      "$ref": "#/components/schemas/Location.PublicRef"
    }
  },
  "required": [
    "id",
    "projectID",
    "cost",
    "tokens",
    "time",
    "location"
  ],
  "additionalProperties": false
}
```

## Session.Message.Info

```json
{
  "anyOf": [
    {
      "$ref": "#/components/schemas/Session.Message.AgentSelected"
    },
    {
      "$ref": "#/components/schemas/Session.Message.ModelSelected"
    },
    {
      "$ref": "#/components/schemas/Session.Message.LocationSwitched"
    },
    {
      "$ref": "#/components/schemas/Session.Message.User"
    },
    {
      "$ref": "#/components/schemas/Session.Message.Synthetic"
    },
    {
      "$ref": "#/components/schemas/Session.Message.System"
    },
    {
      "$ref": "#/components/schemas/Session.Message.Skill"
    },
    {
      "$ref": "#/components/schemas/Session.Message.Shell"
    },
    {
      "$ref": "#/components/schemas/Session.Message.Assistant"
    },
    {
      "$ref": "#/components/schemas/Session.Message.Compaction"
    },
    {
      "$ref": "#/components/schemas/Session.Message.Idle"
    }
  ]
}
```

## Session.Message.Assistant

```json
{
  "type": "object",
  "properties": {
    "id": {
      "type": "string",
      "pattern": "^msg_"
    },
    "metadata": {
      "type": "object"
    },
    "time": {
      "type": "object",
      "properties": {
        "created": {
          "type": "number"
        },
        "streamed": {
          "type": "number"
        },
        "completed": {
          "type": "number"
        }
      },
      "required": [
        "created"
      ],
      "additionalProperties": false
    },
    "type": {
      "type": "string",
      "enum": [
        "assistant"
      ]
    },
    "agent": {
      "type": "string"
    },
    "model": {
      "$ref": "#/components/schemas/Model.Ref"
    },
    "content": {
      "type": "array",
      "items": {
        "anyOf": [
          {
            "$ref": "#/components/schemas/Session.Message.Assistant.Text"
          },
          {
            "$ref": "#/components/schemas/Session.Message.Assistant.Reasoning"
          },
          {
            "$ref": "#/components/schemas/Session.Message.Assistant.Tool"
          }
        ]
      }
    },
    "snapshot": {
      "type": "object",
      "properties": {
        "start": {
          "type": "string"
        },
        "end": {
          "type": "string"
        },
        "files": {
          "type": "array",
          "items": {
            "type": "string"
          }
        }
      },
      "additionalProperties": false
    },
    "finish": {
      "type": "string",
      "enum": [
        "stop",
        "length",
        "tool-calls",
        "content-filter",
        "error",
        "unknown"
      ]
    },
    "rawFinish": {
      "type": "string"
    },
    "providerState": {
      "$ref": "#/components/schemas/Session.Message.ProviderState_4"
    },
    "cost": {
      "$ref": "#/components/schemas/Money.USD"
    },
    "tokens": {
      "$ref": "#/components/schemas/TokenUsage.Info"
    },
    "error": {
      "$ref": "#/components/schemas/Session.StructuredError"
    },
    "retry": {
      "$ref": "#/components/schemas/Session.Message.Assistant.Retry"
    }
  },
  "required": [
    "id",
    "time",
    "type",
    "agent",
    "model",
    "content"
  ],
  "additionalProperties": false
}
```

## Session.Message.User

```json
{
  "type": "object",
  "properties": {
    "id": {
      "type": "string",
      "pattern": "^msg_"
    },
    "metadata": {
      "type": "object"
    },
    "time": {
      "type": "object",
      "properties": {
        "created": {
          "type": "number"
        }
      },
      "required": [
        "created"
      ],
      "additionalProperties": false
    },
    "text": {
      "type": "string"
    },
    "files": {
      "type": "array",
      "items": {
        "$ref": "#/components/schemas/Prompt.FileAttachment"
      }
    },
    "agents": {
      "type": "array",
      "items": {
        "$ref": "#/components/schemas/Prompt.AgentAttachment"
      }
    },
    "skills": {
      "type": "array",
      "items": {
        "$ref": "#/components/schemas/Prompt.SkillAttachment"
      }
    },
    "type": {
      "type": "string",
      "enum": [
        "user"
      ]
    }
  },
  "required": [
    "id",
    "time",
    "text",
    "type"
  ],
  "additionalProperties": false
}
```

## Session.Message.Assistant.Tool

```json
{
  "type": "object",
  "properties": {
    "type": {
      "type": "string",
      "enum": [
        "tool"
      ]
    },
    "id": {
      "type": "string"
    },
    "name": {
      "type": "string"
    },
    "executed": {
      "type": "boolean"
    },
    "providerState": {
      "$ref": "#/components/schemas/Session.Message.ProviderState_2"
    },
    "providerResultState": {
      "$ref": "#/components/schemas/Session.Message.ProviderState_3"
    },
    "state": {
      "anyOf": [
        {
          "$ref": "#/components/schemas/Session.Message.ToolState.Streaming"
        },
        {
          "$ref": "#/components/schemas/Session.Message.ToolState.Running"
        },
        {
          "$ref": "#/components/schemas/Session.Message.ToolState.Completed"
        },
        {
          "$ref": "#/components/schemas/Session.Message.ToolState.Error"
        }
      ]
    },
    "time": {
      "type": "object",
      "properties": {
        "created": {
          "type": "number"
        },
        "ran": {
          "type": "number"
        },
        "completed": {
          "type": "number"
        }
      },
      "required": [
        "created"
      ],
      "additionalProperties": false
    }
  },
  "required": [
    "type",
    "id",
    "name",
    "state",
    "time"
  ],
  "additionalProperties": false
}
```

## Session.Message.ToolState.Completed

```json
{
  "type": "object",
  "properties": {
    "status": {
      "type": "string",
      "enum": [
        "completed"
      ]
    },
    "input": {
      "type": "object"
    },
    "content": {
      "type": "array",
      "prefixItems": [
        {
          "$ref": "#/components/schemas/Tool.Content"
        }
      ],
      "minItems": 1,
      "items": {
        "$ref": "#/components/schemas/Tool.Content"
      }
    },
    "metadata": {
      "type": "object"
    }
  },
  "required": [
    "status",
    "input",
    "content"
  ],
  "additionalProperties": false
}
```

## Permission.Request

```json
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
```

## Permission.Reply

```json
{
  "type": "string",
  "enum": [
    "once",
    "always",
    "reject"
  ]
}
```

## Form.Info

```json
{
  "type": "object",
  "properties": {
    "id": {
      "type": "string",
      "pattern": "^frm_"
    },
    "sessionID": {
      "type": "string"
    },
    "title": {
      "type": "string"
    },
    "metadata": {
      "$ref": "#/components/schemas/Form.Metadata"
    },
    "fields": {
      "$ref": "#/components/schemas/Form.Fields"
    }
  },
  "required": [
    "id",
    "sessionID",
    "title",
    "fields"
  ],
  "additionalProperties": false
}
```

## Form.Reply

```json
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
```

## Agent.Info

```json
{
  "type": "object",
  "properties": {
    "id": {
      "type": "string"
    },
    "name": {
      "type": "string"
    },
    "model": {
      "$ref": "#/components/schemas/Model.Ref"
    },
    "request": {
      "$ref": "#/components/schemas/Provider.Request"
    },
    "system": {
      "type": "string"
    },
    "description": {
      "type": "string"
    },
    "mode": {
      "type": "string",
      "enum": [
        "subagent",
        "primary",
        "all"
      ]
    },
    "hidden": {
      "type": "boolean"
    },
    "color": {
      "$ref": "#/components/schemas/Agent.Color"
    },
    "steps": {
      "type": "integer",
      "exclusiveMinimum": 0
    },
    "permissions": {
      "$ref": "#/components/schemas/Permission.Ruleset"
    }
  },
  "required": [
    "id",
    "name",
    "request",
    "mode",
    "hidden",
    "permissions"
  ],
  "additionalProperties": false
}
```

## Model.Info

```json
{
  "type": "object",
  "properties": {
    "id": {
      "type": "string"
    },
    "modelID": {
      "type": "string"
    },
    "providerID": {
      "type": "string"
    },
    "canonical": {
      "type": "string"
    },
    "family": {
      "type": "string"
    },
    "name": {
      "type": "string"
    },
    "compatibility": {
      "$ref": "#/components/schemas/Model.Compatibility"
    },
    "package": {
      "type": "string"
    },
    "settings": {
      "$ref": "#/components/schemas/Model.Settings"
    },
    "headers": {
      "type": "object",
      "additionalProperties": {
        "type": "string"
      }
    },
    "body": {
      "type": "object"
    },
    "capabilities": {
      "$ref": "#/components/schemas/Model.Capabilities"
    },
    "variants": {
      "type": "array",
      "items": {
        "$ref": "#/components/schemas/Model.Variant"
      }
    },
    "time": {
      "type": "object",
      "properties": {
        "released": {
          "type": "number"
        }
      },
      "required": [
        "released"
      ],
      "additionalProperties": false
    },
    "cost": {
      "type": "array",
      "items": {
        "$ref": "#/components/schemas/Model.Cost"
      }
    },
    "status": {
      "type": "string",
      "enum": [
        "alpha",
        "beta",
        "deprecated",
        "active"
      ]
    },
    "enabled": {
      "type": "boolean"
    },
    "limit": {
      "type": "object",
      "properties": {
        "context": {
          "type": "integer"
        },
        "input": {
          "type": "integer"
        },
        "output": {
          "type": "integer"
        }
      },
      "required": [
        "context",
        "output"
      ],
      "additionalProperties": false
    }
  },
  "required": [
    "id",
    "modelID",
    "providerID",
    "name",
    "capabilities",
    "variants",
    "time",
    "cost",
    "status",
    "enabled",
    "limit"
  ],
  "additionalProperties": false
}
```

## Provider.Info

```json
{
  "type": "object",
  "properties": {
    "id": {
      "type": "string"
    },
    "canonical": {
      "type": "string"
    },
    "integrationID": {
      "type": "string"
    },
    "name": {
      "type": "string"
    },
    "activation": {
      "type": "string",
      "enum": [
        "auto",
        "enabled",
        "disabled"
      ]
    },
    "package": {
      "type": "string"
    },
    "settings": {
      "$ref": "#/components/schemas/Provider.Settings"
    },
    "headers": {
      "type": "object",
      "additionalProperties": {
        "type": "string"
      }
    },
    "body": {
      "type": "object"
    }
  },
  "required": [
    "id",
    "name",
    "activation",
    "package"
  ],
  "additionalProperties": false
}
```

## Mcp.Server

```json
{
  "type": "object",
  "properties": {
    "name": {
      "type": "string"
    },
    "status": {
      "anyOf": [
        {
          "$ref": "#/components/schemas/Mcp.Status.Connected"
        },
        {
          "$ref": "#/components/schemas/Mcp.Status.Pending"
        },
        {
          "$ref": "#/components/schemas/Mcp.Status.Disabled"
        },
        {
          "$ref": "#/components/schemas/Mcp.Status.Failed"
        },
        {
          "$ref": "#/components/schemas/Mcp.Status.NeedsAuth"
        }
      ]
    },
    "integrationID": {
      "type": "string"
    }
  },
  "required": [
    "name",
    "status"
  ],
  "additionalProperties": false
}
```

## FileSystem.Entry

```json
{
  "type": "object",
  "properties": {
    "path": {
      "type": "string"
    },
    "type": {
      "type": "string",
      "enum": [
        "file",
        "directory"
      ]
    }
  },
  "required": [
    "path",
    "type"
  ],
  "additionalProperties": false
}
```

## Project

```json
{
  "type": "object",
  "properties": {
    "id": {
      "type": "string"
    },
    "canonical": {
      "type": "string"
    },
    "vcs": {
      "$ref": "#/components/schemas/Project.Vcs"
    },
    "name": {
      "type": "string"
    },
    "icon": {
      "$ref": "#/components/schemas/Project.Icon"
    },
    "commands": {
      "$ref": "#/components/schemas/Project.Commands"
    },
    "time": {
      "$ref": "#/components/schemas/Project.Time"
    },
    "sandboxes": {
      "type": "array",
      "items": {
        "type": "string"
      }
    }
  },
  "required": [
    "id",
    "canonical",
    "time",
    "sandboxes"
  ],
  "additionalProperties": false
}
```

## TokenUsage.Info

```json
{
  "type": "object",
  "properties": {
    "input": {
      "type": "number"
    },
    "output": {
      "type": "number"
    },
    "reasoning": {
      "type": "number"
    },
    "cache": {
      "type": "object",
      "properties": {
        "read": {
          "type": "number"
        },
        "write": {
          "type": "number"
        }
      },
      "required": [
        "read",
        "write"
      ],
      "additionalProperties": false
    }
  },
  "required": [
    "input",
    "output",
    "reasoning",
    "cache"
  ],
  "additionalProperties": false
}
```

## SessionMessagesResponse

```json
{
  "type": "object",
  "properties": {
    "data": {
      "type": "array",
      "items": {
        "$ref": "#/components/schemas/Session.Message.Info"
      }
    },
    "cursor": {
      "type": "object",
      "properties": {
        "previous": {
          "anyOf": [
            {
              "type": "string"
            },
            {
              "type": "null"
            }
          ]
        },
        "next": {
          "anyOf": [
            {
              "type": "string"
            },
            {
              "type": "null"
            }
          ]
        }
      },
      "additionalProperties": false
    }
  },
  "required": [
    "data",
    "cursor"
  ],
  "additionalProperties": false
}
```

## SessionsResponse

```json
{
  "type": "object",
  "properties": {
    "data": {
      "type": "array",
      "items": {
        "$ref": "#/components/schemas/Session.Info"
      }
    },
    "cursor": {
      "type": "object",
      "properties": {
        "previous": {
          "anyOf": [
            {
              "type": "string"
            },
            {
              "type": "null"
            }
          ]
        },
        "next": {
          "anyOf": [
            {
              "type": "string"
            },
            {
              "type": "null"
            }
          ]
        }
      },
      "additionalProperties": false
    }
  },
  "required": [
    "data",
    "cursor"
  ],
  "additionalProperties": false
}
```

## PromptInput.FileAttachment

```json
{
  "type": "object",
  "properties": {
    "uri": {
      "type": "string"
    },
    "name": {
      "type": "string"
    },
    "description": {
      "type": "string"
    },
    "mention": {
      "$ref": "#/components/schemas/Prompt.Mention"
    }
  },
  "required": [
    "uri"
  ],
  "additionalProperties": false
}
```

## Pty

```json
{
  "type": "object",
  "properties": {
    "id": {
      "type": "string",
      "pattern": "^pty"
    },
    "title": {
      "type": "string"
    },
    "command": {
      "type": "string"
    },
    "args": {
      "type": "array",
      "items": {
        "type": "string"
      }
    },
    "cwd": {
      "type": "string"
    },
    "status": {
      "type": "string",
      "enum": [
        "running",
        "exited"
      ]
    },
    "pid": {
      "type": "integer",
      "minimum": 0
    },
    "exitCode": {
      "type": "integer",
      "minimum": 0
    }
  },
  "required": [
    "id",
    "title",
    "command",
    "args",
    "cwd",
    "status",
    "pid"
  ],
  "additionalProperties": false
}
```
