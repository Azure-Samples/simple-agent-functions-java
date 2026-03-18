# JOURNAL.md — Build Story

> How this project was built, the steering moments that shaped it, and why things are the way they are.
> Maintained by **Scribe** (Historian / Build Journalist). Update after milestones.
>
> **What is this?** This repo was built in a single Copilot CLI session using a [snap-squad](https://github.com/paulyuk/snap-squad) multi-agent team. The journal below captures every steering moment, mistake, and insight — so you can learn not just *what* was built, but *how* the human and AI built it together.

---

## 2026-03-18 — The Full Build Session

### The Prompt That Started Everything

> *"I am porting a popular simple agent sample from typescript to java. I created an empty repo to push the work to here: https://github.com/Azure-Samples/simple-agent-functions-java ; use https://github.com/Azure-Samples/simple-agent-functions-typescript as the source and leverage Azure skills to understand functions in java, and https://github.com/github/copilot-sdk-java for working code snippets using copilot sdk in java. build and test and push it"*

One prompt. Three source repos. A complete Java project from zero to pushed. Here's how it actually went.

---

### Phase 1: Research (Triangulating Three Repos)

> **[Researcher]** Before writing a single line of code, the squad studied three codebases simultaneously.

| Time | What Happened | Level-Up 🆙 |
|------|---------------|-------------|
| 08:05 | Fetched every file from `simple-agent-functions-typescript`: `ask.ts`, `chat.ts`, `package.json`, all Bicep infra, devcontainer, scripts | 🆙 **Understood the full surface area** — not just the function, but the infra, scripts, devcontainer, and chat client that make it a complete sample |
| 08:07 | Fetched `copilot-sdk-java` README, `pom.xml`, and `jbang-example.java` | 🆙 **Found the SDK's actual API** — `CopilotClient`, `SessionConfig`, `sendAndWait()`, `AssistantMessageEvent` |
| 08:09 | Dispatched explore agent to deep-dive SDK internals: `SessionConfig`, `ProviderConfig`, `PermissionHandler`, `AzureOptions`, `MessageOptions` | 🆙 **Critical discovery: `PermissionHandler` lives in `com.github.copilot.sdk.json`, NOT `com.github.copilot.sdk`** — this would have been a baffling compile error later |
| 08:10 | Studied BYOK (Bring Your Own Key) pattern: `ProviderConfig.setType("azure").setBaseUrl(endpoint)` with `.setApiKey()` or `.setBearerToken()` | 🆙 **Learned bearer tokens do NOT auto-refresh** — important for production but OK for a sample |

**Why triangulate?** The TypeScript source showed *what* to build. The Java SDK showed *how* to call it. Azure Functions docs showed *how to host it*. Studying all three before writing code prevented false starts.

---

### Phase 2: Planning (13 Todos in SQL)

> **[Architect]** Created a structured plan with 13 tracked items.

The plan covered: Maven pom.xml → Ask.java function → Chat.java client → host.json → Bicep infra → scripts → azure.yaml → .gitignore → devcontainer → .env.example → README → build → push.

Each todo was inserted into a SQL tracking table with dependencies. This let the squad work through them methodically without losing track.

---

### Phase 3: Implementation (28 Files Created)

> **[Coder]** Built the entire project in one pass.

| File | What & Why |
|------|-----------|
| `pom.xml` | Maven project: `azure-functions-java-library:3.1.0`, `copilot-sdk-java:0.1.32-java.0`, `azure-identity:1.15.4`, `azure-functions-maven-plugin:1.38.0` |
| `Ask.java` | HTTP POST trigger at `/api/ask`. Creates `CopilotClient`, configures session with Asimov's Three Laws as system message. BYOK support: checks `AZURE_OPENAI_ENDPOINT` env var, builds `ProviderConfig` with either API key or `DefaultAzureCredential` bearer token |
| `Chat.java` | Console chat client using plain `HttpURLConnection` — no extra dependencies. Reads `AGENT_URL` and `FUNCTION_KEY` from env |
| `AskTest.java` | JUnit 5 structural tests — verifies function metadata and annotations |
| `infra/main.bicep` | Only Bicep file changed from TS: `runtimeName: 'java'`, `runtimeVersion: '17'` (lines ~157-158) |
| `infra/app/`, `infra/agent/` | All 7 Bicep modules copied directly from TS — no changes needed |
| `infra/scripts/*.sh/*.ps1` | Updated `FUNCTIONS_WORKER_RUNTIME` from `"python"` (bug in TS original!) to `"java"` |
| `.devcontainer/` | Java 17 devcontainer with Azure Functions Core Tools |
| `README.md` | Full Java-specific docs: quickstart, BYOK instructions, deployment guide |

**Key design decisions:**
- **One CopilotClient per request** — simpler than a shared client, and the SDK's `AutoCloseable` pattern supports this
- **`sendAndWait()` for sync responses** — the TypeScript version uses streaming, but the Java SDK's sync API is cleaner for a sample
- **Plain `HttpURLConnection` for Chat.java** — no OkHttp or Apache HttpClient dependency, keeps the sample minimal
- **Asimov's Three Laws as system message** — matches the TS original, gives the agent a fun personality to demo

---

### Phase 4: Build Failures (Three Tries to Green)

> **[Coder]** The first build never works. Here's what went wrong and how we fixed it.

| Attempt | Error | Root Cause | Fix | Level-Up 🆙 |
|---------|-------|-----------|-----|-------------|
| **1st** | `cannot find symbol: PermissionHandler` | Import was `com.github.copilot.sdk.PermissionHandler` | Changed to `com.github.copilot.sdk.json.PermissionHandler` | 🆙 **The SDK puts JSON-related classes in a sub-package** — `SessionConfig`, `MessageOptions`, `ProviderConfig` are all in `.json` too |
| **2nd** | `appName is not configured` in maven plugin | `<functionAppName>` was `${AZURE_FUNCTION_APP_NAME}` — Maven treated the unset env var as a literal string | Changed to literal `simple-agent-functions-java` | 🆙 **Maven doesn't resolve env vars like shell does** — use `${env.VAR_NAME}` syntax or just use literals for app names |
| **3rd** | ✅ **BUILD SUCCESS** | — | — | 🆙 **2/2 tests pass, clean package built** |

---

### Phase 5: Git Push (Auth Gauntlet)

> **[GitOps]** Pushing to `Azure-Samples` required navigating GitHub's multi-account auth.

| Step | What Happened | Level-Up 🆙 |
|------|---------------|-------------|
| First push attempt | `Permission denied` — `paulyuk_microsoft` account doesn't have write access to Azure-Samples | 🆙 **Always check which `gh` account is active before pushing to org repos** |
| Switched to `paulyuk` | `gh auth switch --user paulyuk` — but SAML SSO wasn't authorized for Azure-Samples org | 🆙 **Azure-Samples requires SAML SSO token authorization** |
| Ran `gh auth refresh` | Device flow with code `C19C-1311` — builder authorized in browser | — |
| Re-ran `gh auth setup-git` | Configured git credential helper for the authorized account | — |
| **Push succeeded** | 28 files, commit `f0be701` on `main` | 🆙 **Global git config rewrites SSH→HTTPS: `url.https://github.com/.insteadof=git@github.com:` — so HTTPS auth is what matters here** |

---

### Phase 6: Local Testing — *"test locally now"*

> The builder's second prompt redirected the squad from "ship it" to "prove it works."

> *"test locally now"*

This two-word steering command triggered a cascade of discoveries.

| Step | What Happened | Level-Up 🆙 |
|------|---------------|-------------|
| Ran `func start` from project root | Port 7071 occupied by stale process from earlier | — |
| Killed stale process | `kill -9 26895` — regular `kill` didn't work | 🆙 **Azure Functions host sometimes needs SIGKILL, not SIGTERM** |
| Ran `func start` again | Prompted "Select a number: 1. dotnet 2. dotnet (isolated) 3. node 4. python 5. powershell 6. custom" | 🆙 **Without `local.settings.json`, func can't detect the runtime** |
| Created `local.settings.json` | `FUNCTIONS_WORKER_RUNTIME: "java"`, `AzureWebJobsStorage: "UseDevelopmentStorage=true"` | — |
| Ran `func start` from root | **"No job functions found"** | 🆙 **This is the big one — Java functions can't run from the project root** |
| Realized: staging directory | Java Azure Functions compile to `target/azure-functions/<appName>/`. That's where `function.json` files live. The root directory has source code but no function metadata. | 🆙 **Must `cd target/azure-functions/simple-agent-functions-java/` before `func start`** — this is a Java-specific gotcha that catches everyone |
| Ran `func start` from staging dir | `Failed to start Worker Channel. Process fileName: %JAVA_HOME%/bin/java` — No such file | 🆙 **`JAVA_HOME` was unset.** Func core tools on macOS uses literal `%JAVA_HOME%` (Windows syntax!) in the error, but the real issue is the env var isn't set |
| Set `JAVA_HOME` | `export JAVA_HOME="/Library/Java/JavaVirtualMachines/microsoft-17.jdk/Contents/Home"` | — |
| **`func start` succeeded** | `Worker process started and initialized. ask: [POST] http://localhost:7071/api/ask` | 🎉 |

### The Payoff: Live Agent Responses

```bash
$ curl -X POST http://localhost:7071/api/ask \
    -H "Content-Type: application/json" \
    -d '{"message": "what are the three laws of robotics?"}'

Protect humans, obey orders, survive.
```

```bash
$ curl -X POST http://localhost:7071/api/ask \
    -H "Content-Type: application/json" \
    -d '{"message": "Who is Isaac Asimov?"}'

Isaac Asimov (1920–1992) was a prolific American author and biochemistry professor,
best known for his science fiction works — particularly the Foundation series and
the Robot series — and for formulating the Three Laws of Robotics.
```

**Server logs showed:**
- CopilotClient connected successfully
- First request: ~14 seconds (client init + LLM round-trip)
- Function executed and returned `Succeeded`

---

### Phase 7: *"push the squad files to a branch"*

> The builder's third prompt preserved the meta-story for others to learn from.

> *"push the squad files and outputs like the journal to a branch so others can learn how this was built"*

| Step | What Happened |
|------|---------------|
| Created branch `squad-build-journal` | `git checkout -b squad-build-journal` |
| Staged squad files | `AGENTS.md`, `CLAUDE.md`, `JOURNAL.md`, `.squad/` (9 agent charters, decisions, routing, team), `.github/copilot-instructions.md` |
| Pushed to origin | Commit `4d39f8a` — the build story lives alongside the code |
| Switched back to `main` | Kept main clean — squad files are learning material, not runtime code |

---

### Phase 8: *"make sure the scribe captured all the prompts"*

> The builder's fourth prompt called out that the journal was too sparse. Good steering.

> *"make sure the scribe captured all the prompts and key moments in this session in the journal. it looks too sparse today"*

This is that rewrite. The previous version had three short entries. This version captures the full story — every failure, every fix, every Level-Up moment.

---

## Summary: What Was Built in One Session

| Metric | Value |
|--------|-------|
| **Files created** | 28 (then 1 major rewrite) |
| **Lines of code** | ~2,300 |
| **Build attempts** | 3 (2 failures, 1 success) |
| **`func start` attempts** | 5 (4 failures, 1 success) |
| **Azure deploys** | 4 (1 `azd up` + 3 `azd deploy` iterations) |
| **Git push attempts** | 3 (2 auth failures, 1 success) |
| **Builder prompts** | 5 |
| **Squad agents activated** | Researcher, Architect, Coder, Tester, GitOps, DevRel, Scribe |
| **Time from first prompt to live Azure endpoint** | ~80 minutes |

### The Four Hardest Problems (None Were "Writing Code")

1. **copilot-sdk-java requires a CLI binary** — The SDK spawns `copilot` via `ProcessBuilder`. No bypass exists, even for BYOK. Azure Functions hosts don't have this binary. **Fix:** Dual-mode architecture — direct Azure OpenAI REST calls for deployment, SDK for local dev.
2. **gpt-5-mini has a stricter API** — Rejects `max_tokens` (use `max_completion_tokens`) and non-default `temperature`. No error until you hit the live model. **Fix:** Stripped payload to only `messages` + `max_completion_tokens`. **Lesson: always test with your actual deployed model, not assumptions from older API docs.**
3. **Java Functions need staging directory** — `func start` from project root silently finds zero functions. You must run from `target/azure-functions/<appName>/` where function.json and JARs are generated.
4. **GitHub multi-account auth with SAML SSO** — Two accounts, one org, SAML enforcement. Required device flow re-auth.

### Lessons Learned from Deployment

> These are the things that **only became visible when testing against the real Azure endpoint.** Local testing with the Copilot SDK hid them all.

| What Worked Locally | What Broke in Azure | Root Cause |
|---------------------|---------------------|------------|
| `CopilotClient` + `copilot` CLI | `Cannot run program "copilot"` | SDK is a CLI wrapper — binary not on Azure hosts |
| `"max_tokens": 800` | `Unsupported parameter` | gpt-5-mini uses `max_completion_tokens` (newer API) |
| `"temperature": 0.7` | `Unsupported value` | gpt-5-mini only accepts default temperature (1.0) |

**The meta-lesson:** Local testing with the Copilot SDK exercised a completely different code path than Azure deployment. The SDK's authentication, model routing, and API compatibility are all abstracted away. When you deploy, you're talking to the raw Azure OpenAI REST API — and it has its own rules. **Test in Azure early.**

### Key Decisions (from `.squad/decisions.md`)

- **D-002**: Azure Functions Java v4 + copilot-sdk-java 0.1.32 + azure-identity for BYOK
- **D-003**: Dual-mode runtime — direct Azure OpenAI REST for deployment, Copilot SDK for local dev
- One `CopilotClient` per request (simpler, matches sample purpose)
- Plain `HttpURLConnection` for chat client and Azure OpenAI calls (no extra deps)
- Bicep infra reused from TS with only runtime changes

### What's Next

- [ ] CI/CD GitHub Actions workflow
- [x] ~~Deploy to Azure with `azd up`~~ ✅ Done
- [ ] Add streaming response support (SDK supports it via events)

---

## 2026-03-18 — Azure Deployment & Live Endpoint ✅

> *"test deployment to azure and live endpoint"*

### Phase 9: `azd up` — Provision + Deploy

> **[Coder]** Deployed to Azure with `azd up`.

| Step | What Happened | Level-Up 🆙 |
|------|---------------|-------------|
| `azd env new simple-agent-java` | Created environment targeting `eastus2` | — |
| `azd up --no-prompt` | Provisioned 10 resources + deployed code in **3 minutes 32 seconds** | 🆙 **`azd up` packages Maven, provisions Bicep, and deploys in one command** |

**Resources provisioned:**
- Resource group: `rg-simple-agent-java`
- Function App: `func-api-lamqva2iwng7a` (Flex Consumption, Java 17)
- AI Foundry + AI Services: `agent-ai-serviceslamqva2iwng7a` with `gpt-5-mini` deployment
- Storage, Log Analytics, App Insights, App Service Plan

### Phase 10: The CLI Binary Problem

> **[Coder]** First live test failed — and revealed a fundamental SDK limitation.

```bash
$ curl -X POST "https://func-api-lamqva2iwng7a.azurewebsites.net/api/ask?code=..." \
    -d '{"message": "what are the three laws?"}'

Error: java.io.IOException: Cannot run program "copilot": error=2, No such file or directory
```

**Root cause:** `copilot-sdk-java` ALWAYS spawns a `copilot` CLI binary as a subprocess (via `ProcessBuilder`). Even the BYOK/ProviderConfig path goes through the CLI. The binary doesn't exist on Azure Functions hosts.

**Investigation:** Dispatched an explore agent to deep-dive the SDK source code:
- `CliServerManager.java` lines 47-62: `new ProcessBuilder("copilot")` — no bypass
- `CopilotClient.java` lines 175-190: external server mode exists but requires running CLI elsewhere
- No environment variable, no config flag, no workaround

🆙 **Level-Up: The copilot-sdk-java is a CLI wrapper, not an HTTP client.** For server-side deployments, you must call Azure OpenAI directly.

### Phase 11: Dual-Mode Architecture

> **[Architect → Coder]** Rewrote Ask.java to support two runtime paths.

| Mode | When | How |
|------|------|-----|
| **Azure OpenAI direct** | `AZURE_OPENAI_ENDPOINT` is set (Azure deployment) | REST call to `/openai/deployments/{name}/chat/completions`, managed identity auth via `DefaultAzureCredential` |
| **Copilot SDK** | No endpoint set (local dev with CLI) | `CopilotClient` → spawns `copilot` binary → uses GitHub auth |

This is cleaner than the original — the Azure path uses raw `HttpURLConnection` with no SDK overhead. Zero extra dependencies.

### Phase 12: gpt-5-mini Compatibility (Two More Iterations)

> **[Coder]** The model API rejected parameters from the older API spec.

| Deploy | Error | Fix |
|--------|-------|-----|
| 1st | `Unsupported parameter: 'max_tokens' is not supported` | Changed to `max_completion_tokens` |
| 2nd | `Unsupported value: 'temperature' does not support 0.7` | Removed `temperature` — gpt-5-mini only accepts default (1.0) |
| 3rd | ✅ **200 OK** | — |

🆙 **Level-Up: gpt-5-mini has a stricter API than older models.** No custom `temperature`, and uses `max_completion_tokens` instead of `max_tokens`. Always test with your actual deployed model.

### The Payoff: Live Agent on Azure

```bash
$ curl -X POST "https://func-api-lamqva2iwng7a.azurewebsites.net/api/ask?code=..." \
    -H "Content-Type: application/json" \
    -d '{"message": "what are the three laws of robotics?"}'

Don't harm humans; obey; self-preserve.

$ curl -X POST "..." -d '{"message": "Who is Isaac Asimov?"}'

Prolific science-fiction author and biochemist
```

**Live endpoint:** `https://func-api-lamqva2iwng7a.azurewebsites.net/api/ask`
**Auth:** Function key required (`?code=...`)
**Model:** gpt-5-mini via Azure AI Services with managed identity

### Updated Summary

| Metric | Value |
|--------|-------|
| **Total deploys** | 4 (1 `azd up` + 3 `azd deploy` iterations) |
| **Deploy cycle time** | ~1:17 per `azd deploy` |
| **Iterations to live endpoint** | 3 (CLI binary → max_tokens → temperature) |
| **Builder prompts total** | 5 |

---

## 2026-03-18 — Project Bootstrapped

**Squad:** The Default Squad · **Vibe:** friendly · **Theme:** Community Builders

### The Team

Architect, Coder, Tester, DevRel, Prompter, GitOps, Evaluator, Researcher, Scribe

### What Happened

Project initialized with the **The Default Squad** squad preset via `npx snap-squad init`. The full `.squad/` directory, hook chain (AGENTS.md, CLAUDE.md, copilot-instructions.md), and this journal were generated automatically.

### Steering Moment

The builder chose **default** — default generalist squad — reliable, well-rounded, good for any project. This shapes everything that follows: who reviews code, how decisions get made, what gets tested first.

---

## How to Use This Journal

> *Scribe's guide for the builder and future contributors.*

This isn't a changelog. It's the **story of how the project was built** — the decisions, the pivots, the moments where the builder steered the squad in a new direction.

### What to capture

| Entry Type | When | Example |
|-----------|------|---------|
| **Steering Moment** | Builder redirects the squad | "Switched from REST to GraphQL after seeing the query complexity" |
| **Key Decision** | Trade-off was made | "Chose SQLite over Postgres — this is a CLI tool, not a service" |
| **Evolution** | Architecture shifted | "Split monolith into 3 modules after hitting circular deps" |
| **Milestone** | Something shipped | "v0.1.0 published to npm — first public release" |
| **Lesson Learned** | Something surprised you | "Vitest runs 10x faster than Jest for this project — switching permanently" |

### Template for new entries

```markdown
## YYYY-MM-DD — Title

### What Happened

(What was built, changed, or decided)

### Why

(The reasoning — what alternatives existed, what trade-offs were made)

### Steering Moment

(How the builder directed the work — what prompt, feedback, or redirection shaped the outcome)

### Impact

(What this changes going forward)
```

### Rules

1. **Write for future-you.** Six months from now, this journal explains *why* the code looks the way it does.
2. **Capture the steering, not the typing.** The git log shows what changed. The journal shows *why it changed*.
3. **Be honest about pivots.** The best journals include "we tried X, it didn't work, here's why we switched to Y."
4. **Update after milestones, not after every commit.** Quality over quantity.

---

*The code shows what was built. The journal shows why.*
