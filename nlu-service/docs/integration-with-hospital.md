# Integration With Hospital System

The demo should be integrated as a sidecar NLU service, not as a hard dependency
of the registration workflow.

## Target Flow

```text
patient-wx
  -> patient-wx-api-mysql /agent/multi/chat
  -> TriageAgentWorker
  -> optional nlu-service /infer
  -> ScheduleAgentWorker queries MySQL facts
  -> PolicyAgentWorker validates business rules
  -> ExecutionAgentWorker writes only after server-side confirmation
```

## Suggested Java Configuration

```yaml
agent:
  model-parser:
    enabled: false
    endpoint: http://127.0.0.1:8001/infer
    timeout-ms: 800
    min-confidence: 0.75
```

## Suggested Java Abstraction

```text
ModelIntentParser
  RuleBasedIntentParser
  HttpModelIntentParser
```

Default behavior should stay rule-based. When `enabled=true`, the Java system
can call `/infer` first and use its structured result if confidence is high.

Fallback cases:

- HTTP timeout
- non-200 response
- invalid JSON
- confidence below threshold
- unsupported or unknown intent

In all fallback cases, continue through the existing multi-agent rule path.

