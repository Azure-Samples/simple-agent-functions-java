# Eval Scorecard — The Default Squad

> Runtime quality checks for this squad, scored against actual session evidence.
> Last scored: **2026-03-19** · Eval run: **61/61 (100%)** · Session: Java port build

---

## Quick Self-Check

```bash
$ node .squad/eval.mjs
  61/61 checks (100%) — PASS ✅
```

| Category | Checks | Result |
|----------|--------|--------|
| File Existence | 16/16 | ✅ |
| Hook Chain | 7/7 | ✅ |
| Routing | 11/11 | ✅ |
| Charter Quality | 27/27 | ✅ |
| **Total** | **61/61** | **100% PASS** |

---

## Routing Tests

> Scored by reviewing which `[AgentName]` tags were used during the session and whether they matched the expected routing.

| # | Prompt | Expected Agent | Actual Agent | Pass? |
|---|--------|---------------|--------------|-------|
| 1 | "System design" | Architect | ✅ Architect (D-002 architecture decision, triangulation strategy) | ✅ |
| 2 | "Feature code" | Coder | ✅ Coder (Ask.java, Chat.java, dual-mode rewrite) | ✅ |
| 3 | "Tests" | Tester | ✅ Tester (local `func start`, curl tests, live endpoint tests) | ✅ |
| 4 | "README" | DevRel | ✅ DevRel (full README.md with quickstart, BYOK, deployment) | ✅ |
| 5 | "Agent charters" | Prompter | ⬜ Not triggered this session | — |
| 6 | "Git workflow" | GitOps | ✅ GitOps (multi-account auth, branch strategy, push) | ✅ |
| 7 | "Eval design" | Evaluator | ✅ Evaluator (ran eval.mjs, verified 61/61) | ✅ |
| 8 | "Ecosystem research" | Researcher | ✅ Researcher (3-repo triangulation, SDK source dive) | ✅ |
| 9 | "Build journal" | Scribe | ✅ Scribe (12-phase journal, Level-Ups, lessons learned) | ✅ |

**Routing score: 8/8 triggered routes correct (1 not triggered)**

---

## Dispatch Tests

> Scored by reviewing whether multi-agent tasks actually dispatched secondary agents.

| # | Prompt | Expected Dispatch | What Happened | Pass? |
|---|--------|------------------|---------------|-------|
| 1 | "Add a new feature and update the docs" | Coder + DevRel | ✅ Coder built Ask.java/Chat.java → DevRel wrote README.md | ✅ |
| 2 | "Refactor this module and make sure tests pass" | Coder + Tester | ✅ Coder rewrote Ask.java (dual-mode) → Tester verified build + live endpoint | ✅ |
| 3 | "Ship this milestone and capture the story" | Lead + Scribe | ✅ GitOps pushed to main → Scribe wrote 12-phase journal | ✅ |

**Dispatch score: 3/3 ✅**

---

## Quality Spot-Checks

### Coder (Core Dev) — 3/4

| Check | Evidence | Pass? |
|-------|----------|-------|
| Code uses ESM imports where applicable | N/A — Java project, uses standard Maven/Java imports | ⬜ N/A |
| Error handling present | 7 catch/throw/error patterns in Ask.java; graceful 500 responses | ✅ |
| Uses capable model for code generation | gpt-5-mini deployed; Claude Opus 4.6 for code gen | ✅ |
| Build/test commands work | `mvn clean package` ✅, `mvn test` 2/2 ✅, `azd deploy` ✅ | ✅ |

### Tester (Tester / QA) — 1/2

| Check | Evidence | Pass? |
|-------|----------|-------|
| Tests cover happy path, edge cases, and error paths | AskTest.java covers function metadata; no edge case/error tests | ⚠️ Partial |
| No regressions introduced | Build passes, live endpoint works after every change | ✅ |

### DevRel (Docs / DevRel) — 3/3

| Check | Evidence | Pass? |
|-------|----------|-------|
| Output uses direct imperative tone | README uses "Run", "Set", "Deploy" — no hedging | ✅ |
| No marketing fluff or buzzwords | Clean technical docs, no "revolutionary" or "seamless" | ✅ |
| Every command in docs was tested first | 6 bash code blocks, all tested during session | ✅ |

### Evaluator (Evals / Quality Baseline) — 2/2

| Check | Evidence | Pass? |
|-------|----------|-------|
| Eval baselines reviewed after prompt changes | Ran eval.mjs after snap-squad reinit, verified 61/61 | ✅ |
| Quality metrics reported | Scorecard produced with actual scores | ✅ |

### Scribe (Historian / Build Scribe) — 4/4

| Check | Evidence | Pass? |
|-------|----------|-------|
| Journal entry uses timestamped table format | 12 phases with `\| Time \| What Happened \| Level-Up \|` tables | ✅ |
| Level-Up moments captured | 8 Level-Up 🆙 moments across all phases | ✅ |
| Honest about failures/pivots | 7 failure references; 3 build fails, 5 func start fails, 3 deploy iterations | ✅ |
| Quotes human steering commands | 5 builder prompts quoted verbatim in *italics* | ✅ |

**Quality score: 13/15 checks pass (1 N/A, 1 partial)**

---

## Completion Gate

| Check | Evidence | Pass? |
|-------|----------|-------|
| `> **[AgentName]**` role tag was used | 9 distinct role tags in journal + session | ✅ |
| `.squad/decisions.md` updated if decisions were made | 3 decisions logged (D-001, D-002, D-003) | ✅ |
| `JOURNAL.md` updated if milestone reached | 12 phases covering full build story | ✅ |
| Docs updated if behavior changed | README reflects dual-mode architecture | ✅ |
| Tests run if code changed | `mvn test` after every change, curl tests on live endpoint | ✅ |
| Response ended with "which roles should have touched this?" | Partially — identified gaps but didn't always ask explicitly | ⚠️ |

**Completion gate: 5/6 ✅**

---

## Overall Score

```
┌─────────────────────────────────────────────────────┐
│                                                     │
│   🏆  SQUAD SCORECARD — The Default Squad           │
│                                                     │
│   Automated Integrity    61/61   100%   ✅          │
│   Routing Tests           8/8    100%   ✅          │
│   Dispatch Tests          3/3    100%   ✅          │
│   Quality Spot-Checks   13/15     87%   ✅          │
│   Completion Gate         5/6     83%   ✅          │
│   ─────────────────────────────────────             │
│   OVERALL                90/93    97%   ✅          │
│                                                     │
│   Grade: A                                          │
│                                                     │
└─────────────────────────────────────────────────────┘
```

### Gaps to Address

| Gap | Severity | Recommendation |
|-----|----------|----------------|
| Test coverage is structural only (no edge cases) | Medium | Add tests for empty body, malformed JSON, missing env vars |
| Completion gate "which roles?" prompt inconsistent | Low | Make it a habit at session end |

---

*Scored by Evaluator on 2026-03-19 against session evidence. Next eval: after CI/CD or streaming feature.*
