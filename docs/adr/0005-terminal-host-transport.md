# Terminal↔Host transport: TCP socket, one-way TLS

Host runs as a standalone Kotlin/JVM process (local or remote server) and needs a real transport for Terminal to reach it — this was never actually decided alongside the POS↔Terminal transport. We chose a persistent TCP socket, the same shape as POS↔Terminal, rather than HTTP: this is genuinely how terminal-to-acquirer-host connectivity works in the real world (ISO 8583 predates HTTP-based APIs, and a long-lived socket avoids per-transaction handshake overhead), not a simplification for this project. HTTP/REST is a different layer in real payments (merchant-facing gateway APIs above the ISO 8583 network), not what Host↔Terminal is modeling.

We added one-way TLS on top: Host presents a certificate (self-signed for local dev), Terminal validates it via a pinned truststore. Mutual TLS (Terminal also presenting a client certificate, with the per-terminal provisioning that implies) is deferred — it's more about PKI/provisioning than the transaction lifecycle this project demonstrates, so it joins the other deferred stretch items (deep EMV, USB serial, multi-POS, network fallback).

Status: accepted.
