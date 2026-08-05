# Context Map

## Contexts

- [Host](./host/CONTEXT.md) — simulates the endpoint that authorizes or declines payment transactions
- [Terminal](./terminal/CONTEXT.md) — simulates the payment terminal device, relaying POS transactions to Host over ISO 8583/AS2805
- [POS](./pos/CONTEXT.md) — thin, optional client that triggers and can cancel transactions on Terminal

## Relationships

- **Terminal → Host**: every transaction is presented to Host as an ISO 8583/AS2805 message over a persistent TCP socket (Host as server, Terminal as client), secured with one-way TLS. A Financial under Terminal's Floor Limit is approved offline and reported to Host as a Financial Advice instead. Reversal and Advice retry (3 attempts, 5s interval) using the standard MTI repeat indicator with the same STAN, so Host can respond idempotently. See [ADR-0004](./docs/adr/0004-floor-limit-offline-financial-advice.md) and [ADR-0005](./docs/adr/0005-terminal-host-transport.md).
- **POS → Terminal**: optional. POS is not required for Terminal to operate — Terminal supports standalone, operator-initiated transactions. When POS is present, it sends a Transaction Request (specifying Authorization or Financial) or a Cancel Request; both entry points feed the same internal Terminal flow. See [ADR-0002](./docs/adr/0002-terminal-standalone-before-pos-integration.md).
- **Terminal → POS**: streams Status updates over the life of a transaction (not just a final result), using a coarser vocabulary than Terminal's own internal states. See [ADR-0003](./docs/adr/0003-pos-facing-status-vocabulary.md).
