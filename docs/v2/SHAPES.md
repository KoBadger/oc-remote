
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

## Session.Metadata
```json
{
  "type": "object"
}
```

## Session.Revert
```json
{
  "type": "object",
  "properties": {
    "messageID": {
      "type": "string",
      "pattern": "^msg_"
    },
    "partID": {
      "type": "string"
    },
    "snapshot": {
      "type": "string"
    },
    "files": {
      "type": "array",
      "items": {
        "$ref": "#/components/schemas/FileDiff.Info"
      }
    }
  },
  "required": [
    "messageID"
  ],
  "additionalProperties": false
}
```

## Session.Inbox.Info
```json
{
  "anyOf": [
    {
      "$ref": "#/components/schemas/Session.Inbox.User"
    },
    {
      "$ref": "#/components/schemas/Session.Inbox.Synthetic"
    },
    {
      "$ref": "#/components/schemas/Session.Inbox.Compaction"
    },
    {
      "$ref": "#/components/schemas/Session.Inbox.Move"
    }
  ]
}
```

## Session.Inbox.User
```json
{
  "type": "object",
  "properties": {
    "id": {
      "type": "string",
      "pattern": "^msg_"
    },
    "sessionID": {
      "type": "string",
      "pattern": "^ses"
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
    "type": {
      "type": "string",
      "enum": [
        "user"
      ]
    },
    "payload": {
      "$ref": "#/components/schemas/Session.Inbox.UserPayload"
    },
    "delivery": {
      "$ref": "#/components/schemas/Session.Inbox.Delivery"
    }
  },
  "required": [
    "id",
    "sessionID",
    "time",
    "type",
    "payload",
    "delivery"
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

## Session.Message.Assistant.Text
```json
{
  "type": "object",
  "properties": {
    "type": {
      "type": "string",
      "enum": [
        "text"
      ]
    },
    "text": {
      "type": "string"
    },
    "state": {
      "$ref": "#/components/schemas/Session.Message.ProviderState"
    }
  },
  "required": [
    "type",
    "text"
  ],
  "additionalProperties": false
}
```

## Session.Message.Assistant.Reasoning
```json
{
  "type": "object",
  "properties": {
    "type": {
      "type": "string",
      "enum": [
        "reasoning"
      ]
    },
    "text": {
      "type": "string"
    },
    "state": {
      "$ref": "#/components/schemas/Session.Message.ProviderState_1"
    },
    "time": {
      "type": "object",
      "properties": {
        "created": {
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
    "text"
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

## Session.Message.Assistant.Retry
```json
{
  "type": "object",
  "properties": {
    "attempt": {
      "type": "integer",
      "exclusiveMinimum": 0
    },
    "at": {
      "type": "number"
    },
    "error": {
      "$ref": "#/components/schemas/Session.StructuredError"
    }
  },
  "required": [
    "attempt",
    "at",
    "error"
  ],
  "additionalProperties": false
}
```

## Session.Message.AgentSelected
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
    "type": {
      "type": "string",
      "enum": [
        "agent-switched"
      ]
    },
    "agent": {
      "type": "string"
    },
    "previous": {
      "type": "string"
    }
  },
  "required": [
    "id",
    "time",
    "type",
    "agent"
  ],
  "additionalProperties": false
}
```

## Session.Message.ModelSelected
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
    "type": {
      "type": "string",
      "enum": [
        "model-switched"
      ]
    },
    "model": {
      "$ref": "#/components/schemas/Model.Ref"
    },
    "previous": {
      "$ref": "#/components/schemas/Model.Ref"
    }
  },
  "required": [
    "id",
    "time",
    "type",
    "model"
  ],
  "additionalProperties": false
}
```

## Session.Message.System
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
    "type": {
      "type": "string",
      "enum": [
        "system"
      ]
    },
    "text": {
      "type": "string"
    },
    "description": {
      "type": "string"
    }
  },
  "required": [
    "id",
    "time",
    "type",
    "text"
  ],
  "additionalProperties": false
}
```

## Session.Message.Shell
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
        "shell"
      ]
    },
    "shellID": {
      "type": "string",
      "pattern": "^sh_"
    },
    "command": {
      "type": "string"
    },
    "status": {
      "type": "string",
      "enum": [
        "running",
        "exited",
        "timeout",
        "killed"
      ]
    },
    "exit": {
      "anyOf": [
        {
          "type": "number"
        },
        {
          "type": "string",
          "enum": [
            "Infinity",
            "-Infinity",
            "NaN"
          ]
        }
      ]
    },
    "output": {
      "type": "object",
      "properties": {
        "output": {
          "type": "string"
        },
        "cursor": {
          "type": "integer",
          "minimum": 0
        },
        "size": {
          "type": "integer",
          "minimum": 0
        },
        "truncated": {
          "type": "boolean"
        }
      },
      "required": [
        "output",
        "cursor",
        "size",
        "truncated"
      ],
      "additionalProperties": false
    }
  },
  "required": [
    "id",
    "time",
    "type",
    "shellID",
    "command",
    "status"
  ],
  "additionalProperties": false
}
```

## Session.Message.Synthetic
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
    "description": {
      "type": "string"
    },
    "type": {
      "type": "string",
      "enum": [
        "synthetic"
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

## Session.Message.Skill
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
    "type": {
      "type": "string",
      "enum": [
        "skill"
      ]
    },
    "skill": {
      "type": "string"
    },
    "name": {
      "type": "string"
    },
    "text": {
      "type": "string"
    }
  },
  "required": [
    "id",
    "time",
    "type",
    "skill",
    "name",
    "text"
  ],
  "additionalProperties": false
}
```

## Session.Message.Compaction
```json
{
  "anyOf": [
    {
      "$ref": "#/components/schemas/Session.Message.Compaction.Running"
    },
    {
      "$ref": "#/components/schemas/Session.Message.Compaction.Completed"
    },
    {
      "$ref": "#/components/schemas/Session.Message.Compaction.Failed"
    }
  ]
}
```

## Session.Message.Idle
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
    "type": {
      "type": "string",
      "enum": [
        "idle"
      ]
    },
    "outcome": {
      "type": "string",
      "enum": [
        "succeeded",
        "failed",
        "interrupted"
      ]
    }
  },
  "required": [
    "id",
    "time",
    "type",
    "outcome"
  ],
  "additionalProperties": false
}
```

## Session.Message.ToolState.Running
```json
{
  "type": "object",
  "properties": {
    "status": {
      "type": "string",
      "enum": [
        "running"
      ]
    },
    "input": {
      "type": "object"
    },
    "metadata": {
      "type": "object"
    }
  },
  "required": [
    "status",
    "input",
    "metadata"
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

## Session.Message.ToolState.Error
```json
{
  "type": "object",
  "properties": {
    "status": {
      "type": "string",
      "enum": [
        "error"
      ]
    },
    "input": {
      "type": "object"
    },
    "error": {
      "$ref": "#/components/schemas/Session.StructuredError"
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
    "error"
  ],
  "additionalProperties": false
}
```

## Session.Message.ToolState.Streaming
```json
{
  "type": "object",
  "properties": {
    "status": {
      "type": "string",
      "enum": [
        "streaming"
      ]
    },
    "input": {
      "type": "string"
    }
  },
  "required": [
    "status",
    "input"
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

## SessionActive
```json
{
  "type": "object",
  "properties": {
    "type": {
      "type": "string",
      "enum": [
        "running"
      ]
    }
  },
  "required": [
    "type"
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

## Command.Info
```json
{
  "type": "object",
  "properties": {
    "name": {
      "type": "string"
    },
    "description": {
      "type": "string"
    }
  },
  "required": [
    "name"
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

## Model.Ref
```json
{
  "type": "object",
  "properties": {
    "id": {
      "type": "string"
    },
    "providerID": {
      "type": "string"
    },
    "variant": {
      "type": "string"
    }
  },
  "required": [
    "id",
    "providerID"
  ],
  "additionalProperties": false
}
```

## Model.Variant
```json
{
  "type": "object",
  "properties": {
    "id": {
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
    }
  },
  "required": [
    "id"
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

## Form.Field
```json
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
```

## Form.StringField
```json
{
  "type": "object",
  "properties": {
    "key": {
      "type": "string"
    },
    "title": {
      "type": "string"
    },
    "description": {
      "type": "string"
    },
    "required": {
      "type": "boolean"
    },
    "hidden": {
      "type": "boolean"
    },
    "when": {
      "type": "array",
      "items": {
        "$ref": "#/components/schemas/Form.When"
      }
    },
    "type": {
      "type": "string",
      "enum": [
        "string"
      ]
    },
    "format": {
      "type": "string",
      "enum": [
        "email",
        "uri",
        "date",
        "date-time"
      ]
    },
    "minLength": {
      "type": "integer",
      "minimum": 0
    },
    "maxLength": {
      "type": "integer",
      "minimum": 0
    },
    "pattern": {
      "type": "string"
    },
    "placeholder": {
      "type": "string"
    },
    "default": {
      "type": "string"
    },
    "options": {
      "type": "array",
      "items": {
        "$ref": "#/components/schemas/Form.Option"
      }
    },
    "custom": {
      "type": "boolean"
    }
  },
  "required": [
    "key",
    "type"
  ],
  "additionalProperties": false
}
```

## Form.MultiselectField
```json
{
  "type": "object",
  "properties": {
    "key": {
      "type": "string"
    },
    "title": {
      "type": "string"
    },
    "description": {
      "type": "string"
    },
    "required": {
      "type": "boolean"
    },
    "hidden": {
      "type": "boolean"
    },
    "when": {
      "type": "array",
      "items": {
        "$ref": "#/components/schemas/Form.When"
      }
    },
    "type": {
      "type": "string",
      "enum": [
        "multiselect"
      ]
    },
    "options": {
      "type": "array",
      "items": {
        "$ref": "#/components/schemas/Form.Option"
      }
    },
    "minItems": {
      "type": "integer",
      "minimum": 0
    },
    "maxItems": {
      "type": "integer",
      "minimum": 0
    },
    "custom": {
      "type": "boolean"
    },
    "default": {
      "type": "array",
      "items": {
        "type": "string"
      }
    }
  },
  "required": [
    "key",
    "type",
    "options"
  ],
  "additionalProperties": false
}
```

## Form.ExternalField
```json
{
  "type": "object",
  "properties": {
    "key": {
      "type": "string"
    },
    "type": {
      "type": "string",
      "enum": [
        "external"
      ]
    },
    "url": {
      "type": "string"
    },
    "title": {
      "type": "string"
    },
    "description": {
      "type": "string"
    }
  },
  "required": [
    "key",
    "type",
    "url"
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

## Form.Answer
```json
{
  "type": "object",
  "additionalProperties": {
    "$ref": "#/components/schemas/Form.Value"
  }
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

## Permission.Source
```json
{
  "anyOf": [
    {
      "type": "object",
      "properties": {
        "type": {
          "type": "string",
          "enum": [
            "tool"
          ]
        },
        "messageID": {
          "type": "string"
        },
        "id": {
          "type": "string"
        }
      },
      "required": [
        "type",
        "messageID",
        "id"
      ],
      "additionalProperties": false
    }
  ]
}
```

## REQ session.create POST /api/session
```json
{
  "type": "object",
  "properties": {
    "id": {
      "anyOf": [
        {
          "type": "string",
          "pattern": "^ses"
        },
        {
          "type": "null"
        }
      ]
    },
    "title": {
      "anyOf": [
        {
          "type": "string"
        },
        {
          "type": "null"
        }
      ]
    },
    "agent": {
      "anyOf": [
        {
          "type": "string"
        },
        {
          "type": "null"
        }
      ]
    },
    "model": {
      "anyOf": [
        {
          "$ref": "#/components/schemas/Model.Ref"
        },
        {
          "type": "null"
        }
      ]
    },
    "location": {
      "anyOf": [
        {
          "$ref": "#/components/schemas/Location.PublicRef"
        },
        {
          "type": "null"
        }
      ]
    },
    "metadata": {
      "anyOf": [
        {
          "$ref": "#/components/schemas/Session.Metadata"
        },
        {
          "type": "null"
        }
      ]
    },
    "permissions": {
      "anyOf": [
        {
          "$ref": "#/components/schemas/Permission.Ruleset"
        },
        {
          "type": "null"
        }
      ]
    }
  },
  "additionalProperties": false
}
```

## REQ session.update PATCH /api/session/{sessionID}
```json
{
  "type": "object",
  "properties": {
    "title": {
      "anyOf": [
        {
          "type": "string"
        },
        {
          "type": "null"
        }
      ]
    },
    "metadata": {
      "anyOf": [
        {
          "$ref": "#/components/schemas/Session.Metadata"
        },
        {
          "type": "null"
        }
      ]
    },
    "permissions": {
      "anyOf": [
        {
          "$ref": "#/components/schemas/Permission.Ruleset"
        },
        {
          "type": "null"
        }
      ]
    }
  },
  "additionalProperties": false
}
```

## REQ session.prompt POST /api/session/{sessionID}/prompt
```json
{
  "type": "object",
  "properties": {
    "id": {
      "anyOf": [
        {
          "type": "string",
          "pattern": "^msg_"
        },
        {
          "type": "null"
        }
      ]
    },
    "text": {
      "type": "string"
    },
    "files": {
      "type": "array",
      "items": {
        "$ref": "#/components/schemas/PromptInput.FileAttachment"
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
        "$ref": "#/components/schemas/PromptInput.SkillAttachment"
      }
    },
    "metadata": {
      "type": "object"
    },
    "delivery": {
      "anyOf": [
        {
          "$ref": "#/components/schemas/Session.Inbox.Delivery"
        },
        {
          "type": "null"
        }
      ]
    },
    "resume": {
      "anyOf": [
        {
          "type": "boolean"
        },
        {
          "type": "null"
        }
      ]
    }
  },
  "required": [
    "text"
  ],
  "additionalProperties": false
}
```

## REQ session.switchModel POST /api/session/{sessionID}/model
```json
{
  "type": "object",
  "properties": {
    "model": {
      "$ref": "#/components/schemas/Model.Ref"
    }
  },
  "required": [
    "model"
  ],
  "additionalProperties": false
}
```

## REQ session.switchAgent POST /api/session/{sessionID}/agent
```json
{
  "type": "object",
  "properties": {
    "agent": {
      "type": "string"
    }
  },
  "required": [
    "agent"
  ],
  "additionalProperties": false
}
```

## REQ session.command POST /api/session/{sessionID}/command
```json
{
  "type": "object",
  "properties": {
    "name": {
      "type": "string"
    },
    "text": {
      "type": "string"
    },
    "files": {
      "type": "array",
      "items": {
        "$ref": "#/components/schemas/PromptInput.FileAttachment"
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
        "$ref": "#/components/schemas/PromptInput.SkillAttachment"
      }
    },
    "delivery": {
      "anyOf": [
        {
          "$ref": "#/components/schemas/Session.Inbox.Delivery"
        },
        {
          "type": "null"
        }
      ]
    }
  },
  "required": [
    "name",
    "text"
  ],
  "additionalProperties": false
}
```

## REQ session.shell POST /api/session/{sessionID}/shell
```json
{
  "type": "object",
  "properties": {
    "id": {
      "anyOf": [
        {
          "type": "string",
          "pattern": "^msg_"
        },
        {
          "type": "null"
        }
      ]
    },
    "command": {
      "type": "string"
    }
  },
  "required": [
    "command"
  ],
  "additionalProperties": false
}
```

## REQ session.permission.reply POST /api/session/{sessionID}/permission/{requestID}/reply
```json
{
  "type": "object",
  "properties": {
    "decision": {
      "$ref": "#/components/schemas/Permission.Reply"
    },
    "message": {
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
  "required": [
    "decision"
  ],
  "additionalProperties": false
}
```

## REQ session.form.reply POST /api/session/{sessionID}/form/{formID}/reply
```json
{
  "$ref": "#/components/schemas/Form.Reply"
}
```

## REQ session.synthetic POST /api/session/{sessionID}/synthetic
```json
{
  "type": "object",
  "properties": {
    "id": {
      "anyOf": [
        {
          "type": "string",
          "pattern": "^msg_"
        },
        {
          "type": "null"
        }
      ]
    },
    "text": {
      "type": "string"
    },
    "description": {
      "anyOf": [
        {
          "type": "string"
        },
        {
          "type": "null"
        }
      ]
    },
    "metadata": {
      "type": "object"
    },
    "delivery": {
      "anyOf": [
        {
          "$ref": "#/components/schemas/Session.Inbox.Delivery"
        },
        {
          "type": "null"
        }
      ]
    },
    "resume": {
      "anyOf": [
        {
          "type": "boolean"
        },
        {
          "type": "null"
        }
      ]
    }
  },
  "required": [
    "text"
  ],
  "additionalProperties": false
}
```