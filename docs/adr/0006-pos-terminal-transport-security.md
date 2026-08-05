# POS↔Terminal transport security: one-way TLS, Terminal as server

The POS↔Terminal transport (socket/WiFi) was left without a security decision when it was originally scoped — only Terminal↔Host got one (ADR-0005). Leaving it unsecured wasn't a deliberate choice, just a gap the domain-modeling session missed, and it was flagged as an open question in the Terminal (POS integration) and POS Simulator specs (#3, #4).

We decided to close that gap with the same shape as Terminal↔Host: one-way TLS. Terminal is the server (POS connects into it, matching the WiFi-AP-style pattern the transport is modeled on) and presents a certificate (self-signed for local dev); POS validates it against a pinned truststore. Mutual TLS is deferred for the same reason it was deferred for Terminal↔Host — POS is a thin, optional client, and client-certificate provisioning would be disproportionate effort for a demo. Keeping both links at the same security shape (one-way TLS, deferred mTLS) is also simpler to reason about and implement than treating them differently without a real reason to.

Status: accepted.
