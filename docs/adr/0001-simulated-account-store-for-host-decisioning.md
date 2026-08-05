# Simulated account store for Host approve/decline decisioning

Host needs some basis for approving or declining a Transaction. We considered a simple amount-based rule (e.g. decline above a threshold) against an in-memory simulated account store (PAN → balance/limit) with real approve/decline logic. We chose the account store: it makes the Authorization-vs-Financial distinction behaviorally real (hold vs. debit), gives Reversal actual state to undo, and better fits the project's goal of demonstrating host-side transaction processing rather than a protocol echo chamber.

Status: accepted, with an explicit fallback — if the account store proves too time-consuming, drop back to the amount-based rule.
