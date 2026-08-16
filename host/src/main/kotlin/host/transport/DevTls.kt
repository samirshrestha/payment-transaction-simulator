package host.transport

/**
 * Self-signed TLS material for local dev, per ADR-0005 (Terminal<->Host transport: TCP socket,
 * one-way TLS). Not a real secret — `host-keystore.p12` is a demo cert checked into the repo, and
 * `changeit` is its well-known password. Real deployment would externalize this.
 */
object DevTls {
    const val KEYSTORE_RESOURCE = "/tls/host-keystore.p12"
    const val KEYSTORE_PASSWORD = "changeit"
}
