# Domain Docs

How the engineering skills should consume this repo's domain documentation when exploring the codebase.

## Before exploring, read these

- **`CONTEXT-MAP.md`** at the repo root — points at one `CONTEXT.md` per context (`host/`, `terminal/`, `pos/`). Read each one relevant to the topic.
- **`docs/adr/`** — system-wide decisions; read any that touch the area you're about to work in.

This repo has no per-context `docs/adr/` yet — all ADRs so far are system-wide, at the root `docs/adr/`. If a context-specific ADR is ever needed, it belongs at `<context>/docs/adr/` (e.g. `host/docs/adr/`), matching the multi-context layout below.

## File structure

Multi-context repo:

```
/
├── CONTEXT-MAP.md
├── docs/adr/          ← system-wide decisions
├── host/
│   └── CONTEXT.md
├── terminal/
│   └── CONTEXT.md
└── pos/
    └── CONTEXT.md
```

## Use the glossary's vocabulary

When your output names a domain concept (in an issue title, a refactor proposal, a hypothesis, a test name), use the term as defined in the relevant `CONTEXT.md` — e.g. Authorization / Financial / Reversal / Account / STAN / Floor Limit / Financial Advice (host and terminal), Status (POS). Don't drift to synonyms the glossary explicitly avoids.

If the concept you need isn't in the glossary yet, that's a signal — either you're inventing language the project doesn't use (reconsider) or there's a real gap (note it for `/domain-modeling`).

## Flag ADR conflicts

If your output contradicts an existing ADR, surface it explicitly rather than silently overriding:

> _Contradicts ADR-0001 (simulated account store) — but worth reopening because…_
