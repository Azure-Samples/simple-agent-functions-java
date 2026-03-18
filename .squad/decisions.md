# Decisions — The Default Squad

> Significant decisions made during development. Check before starting work.

## Active Decisions

### D-001: Squad initialized with The Default Squad preset
- **By:** snap-squad
- **Date:** 2026-03-18
- **Context:** Project initialized using snap-squad warm-start
- **Decision:** Using the "default" preset (friendly vibe, Community Builders theme)

### D-002: Java port architecture — Azure Functions v4 + copilot-sdk-java
- **By:** Architect/Coder
- **Date:** 2026-03-18
- **Context:** Porting simple-agent-functions from TypeScript to Java
- **Decision:** Use Azure Functions Java v4 (annotation-based), Maven build, copilot-sdk-java 0.1.32 from Maven Central, azure-identity for BYOK DefaultAzureCredential. Reuse all Bicep infra from TS version with runtime changed to java/17. Chat client implemented as Java main class.

### D-003: Dual-mode runtime — direct Azure OpenAI + Copilot SDK fallback
- **By:** Architect/Coder
- **Date:** 2026-03-18
- **Context:** copilot-sdk-java requires `copilot` CLI binary in PATH (spawns it via ProcessBuilder). Binary not available on Azure Functions hosts.
- **Decision:** Ask.java supports two modes: (1) Azure OpenAI direct REST calls when AZURE_OPENAI_ENDPOINT is set (used in Azure deployment, managed identity auth), (2) Copilot SDK fallback when no endpoint is configured (local dev with CLI). Direct mode uses plain HttpURLConnection — no extra dependencies.
