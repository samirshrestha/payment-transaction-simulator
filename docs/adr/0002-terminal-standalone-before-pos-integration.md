# Terminal supports standalone operation; POS is an optional, later-arriving trigger

POS is an optional entity in this system — a real terminal can be operated on its own. We decided Terminal must support starting a transaction itself (operator enters amount + Authorization/Financial on-device) rather than requiring a POS-originated request. Both entry points feed the same internal transaction flow; POS, once built, is just an alternate trigger source, not a different code path.

This also resolves the build-order dependency implied by `CLAUDE.md`'s sequencing (Host → Terminal → POS): Terminal is fully testable end-to-end before POS Simulator exists, since standalone mode doesn't depend on it.

Status: accepted.
