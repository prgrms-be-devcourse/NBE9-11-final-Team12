# Prompt Guard

This container runs the official `seojoonkim/prompt-guard` project as the
internal prompt safety API for Spring Boot custom report prompts.

Repository:

- https://github.com/seojoonkim/prompt-guard

## Run

```powershell
docker compose up -d prompt-guard
```

When Spring Boot runs on the host machine, set:

```text
PROMPT_GUARD_BASE_URL=http://localhost:18080
PROMPT_GUARD_FAIL_OPEN=false
```

When Spring Boot runs in the same Docker Compose network, set:

```text
PROMPT_GUARD_BASE_URL=http://prompt-guard:8080
PROMPT_GUARD_FAIL_OPEN=false
```

## Check

```powershell
Invoke-RestMethod `
  -Method Post `
  -Uri http://localhost:18080/scan `
  -ContentType "application/json" `
  -Body '{"content":"hello","type":"analyze"}'
```

Expected safe response shape:

```json
{
  "action": "allow",
  "blocked": false,
  "was_modified": false,
  "sanitized_text": null,
  "matches": []
}
```

## Policy

- The container installs and runs the official upstream project during Docker build.
- `config.yaml` sets `HIGH` and `CRITICAL` detections to `block`.
- External pattern/reporting features are disabled for local deterministic behavior.
- The service is exposed only on `127.0.0.1:18080` by Docker Compose for local development.
- Spring Boot still calls `/scan` through `HttpPromptGuardService`; no entity or prompt content is logged by the backend.
