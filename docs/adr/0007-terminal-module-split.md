# Terminal splits into domain/data/ui/app Gradle modules

`:terminal` was going to be one Android module with Clean-Architecture-shaped internal packages (the engine decoupled from UI and transport via dependency inversion — `HostClient`/`CardReader`/`PosGateway` as interfaces owned by the domain layer). We decided to make that boundary a real Gradle module boundary instead: **`:terminal:domain`** (pure Kotlin/JVM — the engine, `TerminalState`, the collaborator interfaces, retry/idempotency logic), **`:terminal:data`** (concrete implementations: real `HostClient`, `CardReader`, `PosGateway`), **`:terminal:ui`** (Compose screens, ViewModel), **`:terminal:app`** (thin Android shell — `Application`, Hilt wiring, `MainActivity`).

This is scoped to Terminal only — Host and POS stay single modules. Host's structure (wire format + domain + transport) is already one curated flow without the same layering; POS is explicitly thin. Splitting those further would be modularity without reflecting real internal structure.

Why now rather than leaving it as packages: `:terminal:domain` having zero Android dependency is enforced by the build, not just convention, so the "engine is testable with no Android framework involved" claim (already the project's stated testing approach) becomes a structural fact rather than a discipline someone could accidentally violate. It also makes "modular architecture" — something job postings frequently list as a separate line item from "Clean Architecture" — literally true of the repo, not just true in spirit.

`:terminal:app` is a genuinely thin module (DI wiring + `MainActivity` + manifest) — cheap to set up, kept separate from `:terminal:ui` so `:ui` stays a pure screens/ViewModel library with no app-shell concerns in it.

Status: accepted. Established in the "Terminal: engine core" ticket (module skeletons for all four, with dependency wiring: `:app` → `:domain`, `:data`, `:ui`; `:ui` → `:domain`; `:data` → `:domain`), filled in by the tickets that follow.
