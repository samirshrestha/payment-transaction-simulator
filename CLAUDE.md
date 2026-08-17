\# Payment Transaction Simulator — Project Context



\## Scope

Simulates a payment transaction lifecycle (Terminal -> Host, with POS as an optional,

later-arriving trigger — Terminal also supports standalone operator-initiated transactions)

for demonstration purposes. Host-side logic implements the \*published\* ISO 8583/AS2805 standard (no

employer-proprietary logic). Crypto/key handling (DUKPT/MAC/PIN block) and EMV logic are

conceptual simulations for demonstration purposes — not production-grade or certified

implementations. POS-facing schema is an original design, not modeled on any real

POS-to-terminal protocol.



\## Stack

\- Host Simulator: pure Kotlin/JVM, runs as a TCP socket server (local or remote), one-way TLS

\- Terminal App: Android, Jetpack Compose (not Compose Multiplatform) + Coroutines/Flow,

&#x20; ViewModel/StateFlow, Hilt (DI), Navigation Compose, Material 3. Split into 4 Gradle modules

&#x20; (\`:terminal:domain\`, \`:terminal:data\`, \`:terminal:ui\`, \`:terminal:app\`) — Host and POS stay

&#x20; single modules, this split is Terminal-specific. See \[ADR-0007](./docs/adr/0007-terminal-module-split.md).

\- Terminal <-> Host transport: persistent TCP socket, one-way TLS (Terminal validates Host's

&#x20; cert). Mutual TLS is a deferred stretch item, not core scope.

\- POS <-> Terminal transport: pluggable interface, socket/WiFi implementation built first

&#x20; (reflects real WiFi+Serial support at a past role), one-way TLS (Terminal as server, POS

&#x20; validates Terminal's cert). Mutual TLS is a deferred stretch item, not core scope. Serial via

&#x20; the usb-serial-for-android library (CDC/ACM support) is a legitimate OPTIONAL stretch, not

&#x20; core scope — the API itself is genuinely simple, but it needs real USB-to-serial hardware to

&#x20; test (unlike everything else in this project) and doesn't deepen the Compose/Coroutines gap

&#x20; that's the actual point of the Terminal App. Only build it if core scope is done with time to

&#x20; spare.

\- POS Simulator: thin Kotlin/JVM client, console/CLI for v1 (Compose Multiplatform GUI is a

&#x20; deferred stretch item, not core scope)



\## Workflow

\- Before implementing a new concept, briefly explain the underlying idea first, then implement.

\- Discipline for every feature: propose an approach -> let me question/challenge it ->

&#x20; implement -> add solid test coverage -> manual testing -> only then automate the PR step.

\- Pairing split (agreed after ticket #5): mechanical scaffolding (build config, directory setup,

&#x20; cert generation, boilerplate with no decisions in it) — agent does it outright, no discussion

&#x20; needed. Anything with an actual design decision (algorithms, wire-format/protocol logic,

&#x20; control flow, validation rules) — pair on it: agent explains the concept and proposes a

&#x20; signature/failing-test shape, human writes the implementation, agent reviews/tests before the

&#x20; next slice. One seam at a time, per the TDD discipline above.

\- If CodeRabbit hasn't reviewed an open PR automatically, request one by commenting

&#x20; `@coderabbitai full review` (or `@coderabbitai review` for an incremental pass). Use

&#x20; `@coderabbitai rate limit` before re-triggering when review capacity is uncertain.

\- After a significant milestone, ask whether any external reference documents should be updated.

\- \*\*Future idea (not set up yet):\*\* branch protection on \`main\` — gate merges behind a free AI

&#x20; code reviewer (e.g. CodeRabbit) and Snyk dependency/vulnerability scanning instead of merging

&#x20; directly. Also CI/CD to build artifacts after a PR merges to \`main\`. Not designed yet.



\## Sequencing

Host Simulator first, then Terminal App (standalone/operator-initiated mode first,

POS integration after), then POS Simulator.



Architecture decisions (module boundaries, tech choices): see \[CONTEXT-MAP.md](./CONTEXT-MAP.md),

per-context \[CONTEXT.md](./host/CONTEXT.md) files, and \[docs/adr](./docs/adr) for the domain

model and decisions captured in the first grilling/domain-modeling session.



\## Agent skills



\### Issue tracker



Issues and specs live as GitHub issues in `samirshrestha/payment-transaction-simulator` (uses the `gh` CLI). See `docs/agents/issue-tracker.md`.



\### Triage labels



Default five canonical labels (`needs-triage`, `needs-info`, `ready-for-agent`, `ready-for-human`, `wontfix`). See `docs/agents/triage-labels.md`.



\### Domain docs



Multi-context — \[CONTEXT-MAP.md](./CONTEXT-MAP.md) plus one `CONTEXT.md` per context (`host/`, `terminal/`, `pos/`). See `docs/agents/domain.md`.

