# Floor-limit offline approval for Financial, with retried Advice/Reversal delivery

Financial Advice (informing Host of a transaction that already completed) only makes sense if Terminal can approve something without asking Host first — which the shallow-EMV decision had ruled out as "kernel-level offline/online decision logic." We narrowed this rather than re-scoping to deep EMV: Terminal approves a **Financial** transaction offline only via a simple **Floor Limit** (amount < $50 AUD), a single comparison, not a cryptographic or CVM decision. Authorization is unaffected — it always goes through the online flow.

An offline-approved Financial sends a fire-and-forget Financial Advice to Host and reaches `Approved` immediately, regardless of whether the Advice send succeeds — the sale already happened at the point of sale. A failed Advice is a logged reconciliation gap, not a state-machine outcome.

We also decided Reversal and Advice (but not the initial online transaction request) get retry: 3 attempts, fixed 5s interval. Since retries can arrive after Host already processed the original, we use ISO 8583's standard mechanism to disambiguate — the MTI's origin/repeat digit (odd = repeat) — with the **same STAN** kept across retries, so Host can detect "already processed this one" and respond idempotently instead of double-applying the Account change.

Status: accepted.
