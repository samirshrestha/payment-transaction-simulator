# POS receives a streamed, collapsed status vocabulary — not Terminal's internal states 1:1

POS wants visibility into what Terminal is doing, not just a final result — so the POS↔Terminal protocol streams status updates over the life of a transaction rather than a single request/response. We decided POS sees a separate, coarser status vocabulary (`Reading`, `Processing`, `Approved`, `Declined`, `Reversed`, `Cancelled`, `Failed`) rather than Terminal's literal internal states (`CardReading`, `CardPresented`, `Authorizing`, `Reversing`, `ConnectionFailed`, `ReversalFailed`). This keeps Terminal's internal state machine free to evolve without breaking the POS-facing protocol.

Status: accepted.
