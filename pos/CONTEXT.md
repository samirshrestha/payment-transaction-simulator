# POS

A thin, optional client that triggers transactions on Terminal and cancels them. Not required for Terminal to function — see [ADR-0002](../docs/adr/0002-terminal-standalone-before-pos-integration.md). Connects to Terminal over a TCP socket secured with one-way TLS (Terminal as server) — see [ADR-0006](../docs/adr/0006-pos-terminal-transport-security.md).

## Language

**Transaction Request**:
What POS sends to start a transaction on Terminal: `amount` + transaction type (Authorization or Financial). No card/PAN data — card presentment is entirely Terminal's domain.

**Cancel Request**:
What POS sends to abort a transaction it started, before Terminal reaches `Authorizing`. See Terminal's `Cancelled` state.

**Status**:
The POS-facing view of a transaction's progress, streamed over the life of the transaction rather than delivered as a single final result. Deliberately coarser than Terminal's internal states — see [ADR-0003](../docs/adr/0003-pos-facing-status-vocabulary.md):
- `Reading` — Terminal's `CardReading` + `CardPresented`
- `Processing` — Terminal's `Authorizing` + `Reversing`
- `Approved`, `Declined`, `Reversed`, `Cancelled` — 1:1 with Terminal
- `Failed` — Terminal's `ConnectionFailed` + `ReversalFailed`
_Avoid_: exposing Terminal's internal state names directly to POS

## Scope (v1)

One POS connected to one Terminal at a time, one Transaction Request in flight at a time — matches Terminal's single-threaded processing.

POS is a plain JVM console/CLI app for v1 — not a GUI. It's a test harness for Terminal, not a product in its own right.

**Future idea (not v1 scope):** a real GUI for POS using Compose Multiplatform, kept in the backlog specifically so POS could later deploy to any platform. Not designed yet; would be its own deliberate stretch goal, same tier as USB serial support in Terminal.

**Future idea (not v1 scope):** multiple POS clients (e.g. different workers) connected to a single Terminal — would need real connection management at the transport layer, plus a policy (queue vs. reject) for a Transaction Request arriving while Terminal is already busy. Deferred; not designed yet.

**Future idea (not v1 scope):** mutual TLS for POS↔Terminal (POS also presents a client certificate). v1 is one-way TLS only — see [ADR-0006](../docs/adr/0006-pos-terminal-transport-security.md).
