# Terminal

Simulates the payment terminal device: starts transactions (either standalone, operator-initiated, or relayed from POS), presents them to Host as ISO 8583/AS2805 messages over a TLS-secured TCP socket, and simulates the card-present flow (EMV) in between. Runs as an Android app (physical device or emulator). Acts as a TLS server for POS (POS connects into Terminal) and as a TLS client to Host (Terminal connects into Host) — one-way TLS in both directions.

## Language

**Transaction Origin**:
A transaction can start two ways: **standalone** (Terminal operator enters amount + Authorization/Financial directly on-device) or **POS-initiated** (POS sends the request, specifying the type). Both feed the same internal transaction flow — POS is an optional, alternate trigger source, not a separate code path. See [ADR-0002](../docs/adr/0002-terminal-standalone-before-pos-integration.md).
_Avoid_: assuming POS is required to operate Terminal

**EMV depth (v1 scope)**:
Shallow EMV shape — conceptual steps (application/AID selection, a fake cryptogram, contactless vs. contact distinction) are simulated to exercise the state machine and vocabulary, but with no real cryptographic correctness and no kernel-level decision logic (offline/online auth, CVM selection). Deep EMV shape is a deferred stretch/learning exercise, not core v1 scope.

**Host Timeout**:
The window Terminal waits for a response to an Authorization or Financial request before treating it as failed and automatically firing a Reversal (targeting the STAN it just sent). Hardcoded at 10 seconds for v1 — a demo-friendly value, not a real-world one, and not configurable. This is the primary trigger that exercises Reversal end-to-end.

**Floor Limit**:
A hardcoded $50 AUD threshold, Financial only. A Financial transaction under the Floor Limit is approved offline — a single amount comparison, not a kernel/cryptographic decision — bypassing `Authorizing` entirely. Deliberately narrow: does not apply to Authorization, and is not a step toward deep EMV decision logic. See [ADR-0004](../docs/adr/0004-floor-limit-offline-financial-advice.md).

**Financial Advice**:
What Terminal sends Host after an offline-approved Financial (fire-and-forget, distinct from the online Financial request/response pair). Host cannot decline it — the transaction already happened at the point of sale. The transaction stays `Approved` regardless of whether the Advice send succeeds; a failed Advice is a logged reconciliation gap, not a state-machine outcome.
_Avoid_: treating Advice like a request that could be declined

## Transaction Lifecycle (v1 scope)

```
Idle → CardReading → CardPresented → Authorizing → Approved
          ↓               ↓  ↓            ↓ (declined)
      Cancelled     Cancelled  (Financial, amount < Floor Limit)
                                  ↓                Declined
                          send Financial Advice*
                                  ↓
                              Approved

                                          Authorizing, no response within Host Timeout
                                                          ↓
                                                      Reversing* → Reversed
                                          ↓ (couldn't send at all)      ↓ (retries exhausted)
                                    ConnectionFailed              ReversalFailed

  * Reversal and Financial Advice retry: 3 attempts, fixed 5s interval, same STAN
    with the MTI repeat indicator flipped — see Retry & Idempotency below.
```

- **Idle**: no transaction in progress, waiting for a Transaction Origin — either the operator starting one on-device (standalone) or POS sending a request.
- **CardReading**: shallow-EMV card flow in progress (application/AID selection, cryptogram stand-in being generated).
- **CardPresented**: PAN/expiry + cryptogram stand-in ready. Branches here: Financial under the Floor Limit goes offline (sends a Financial Advice, then straight to `Approved`); everything else proceeds to `Authorizing`.
- **Cancelled**: aborted before `Authorizing` (or before the offline branch fires) — triggerable both by the Terminal operator (device-side UI) and by POS (a cancel message over the POS↔Terminal protocol). Not reachable once a request has been sent to Host — a transaction in flight cannot be cancelled, only reversed.
- **Authorizing**: request sent to Host, awaiting response. Host Timeout (10s) applies here. Not entered at all for an offline-approved Financial.
- **Approved** / **Declined**: terminal states reflecting Host's response (online) or an offline approval under the Floor Limit.
- **ConnectionFailed**: the *initial* request to Host could not be sent at all (no network/socket refused) — distinct from a timeout, where the request *was* sent but no response came back. One-shot, no retry — the customer is standing there waiting, so Terminal fails fast rather than retrying.
- **Reversing**: Host Timeout fired after a successful send; Terminal is sending/awaiting the Reversal, retrying as needed.
- **Reversed**: Reversal acknowledged by Host.
- **ReversalFailed**: all Reversal retry attempts exhausted — Host unreachable throughout. Terminal state; may need manual/operator follow-up (out of scope to resolve automatically in v1).

All of `Cancelled`, `ConnectionFailed`, `Approved`, `Declined`, `Reversed`, `ReversalFailed` are terminal — Terminal relays the result to POS and returns to `Idle` for the next transaction.

## Retry & Idempotency (v1 scope)

The *initial* transaction request (the one that can hit `ConnectionFailed`) is one-shot — no retry. **Reversal and Financial Advice retry: 3 attempts, fixed 5s interval** — both represent something that already happened and needs to eventually reach Host, unlike the initial request where a customer is actively waiting.

Retries reuse the **same STAN** as the original attempt (never generate a new one) and set ISO 8583's standard MTI repeat indicator (the origin digit — odd value = repeat) rather than inventing a custom retry field. Host uses STAN + repeat flag to recognize a retry of something it may have already processed and responds idempotently (re-acknowledges without re-applying the Account change). See [ADR-0004](../docs/adr/0004-floor-limit-offline-financial-advice.md).

**Future idea (not v1 scope):** network fallback for the Terminal↔Host link — if WiFi is down, fall back to a SIM/cellular connection. Would build on the transport already being pluggable; not designed yet.

**Future idea (not v1 scope):** mutual TLS for Terminal↔Host (Terminal also presents a client certificate). v1 is one-way TLS only — see [ADR-0005](../docs/adr/0005-terminal-host-transport.md).

**Future idea (not v1 scope):** mutual TLS for POS↔Terminal (POS also presents a client certificate). v1 is one-way TLS, Terminal as server — see [ADR-0006](../docs/adr/0006-pos-terminal-transport-security.md).

**Future idea (not v1 scope):** a Store backend — a third actor beyond Host and POS, loosely modeled on the role Google Play plays for a real Android terminal — that Terminal authenticates to (OAuth client-credentials or device-authorization grant, since Terminal is a machine, not a human logging in) and pulls remote config from (e.g. Host endpoint, feature flags, trust anchors). Surfaced while assessing job-posting fit: v1's Terminal↔Host and POS↔Terminal links are both TLS-secured sockets with no token exchange, so this would be the first place OAuth shows up in the project. Not designed yet — would need its own CONTEXT.md if it goes beyond a stub.

## Module Structure

`:terminal` splits into four Gradle modules: `:terminal:domain` (pure Kotlin/JVM — the engine, `TerminalState`, the `HostClient`/`CardReader`/`PosGateway` interfaces, retry/idempotency logic), `:terminal:data` (concrete implementations of those interfaces), `:terminal:ui` (Compose screens, ViewModel), `:terminal:app` (thin Android shell — DI wiring, `MainActivity`, manifest). Scoped to Terminal only — Host and POS stay single modules, since neither has the same layering to make literal. See [ADR-0007](../docs/adr/0007-terminal-module-split.md).
