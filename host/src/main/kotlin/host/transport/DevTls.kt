package host.transport

import java.security.KeyStore
import javax.net.ssl.SSLContext
import javax.net.ssl.SSLSocketFactory
import javax.net.ssl.TrustManagerFactory

/**
 * Self-signed TLS material for local dev, per ADR-0005 (Terminal<->Host transport: TCP socket,
 * one-way TLS). Not a real secret — `host-keystore.p12` is a demo cert checked into the repo, and
 * `changeit` is its well-known password. Real deployment would externalize this.
 */
object DevTls {
    const val KEYSTORE_RESOURCE = "/tls/host-keystore.p12"
    const val TRUSTSTORE_RESOURCE = "/tls/host-truststore.p12"
    const val KEYSTORE_PASSWORD = "changeit"

    /** Builds a client [SSLSocketFactory] trusting the dev cert -- shared by tests and [host.tools.ManualTestClient]. */
    fun clientSocketFactory(): SSLSocketFactory {
        val trustStore = KeyStore.getInstance("PKCS12")
        val trustStoreStream = DevTls::class.java.getResourceAsStream(TRUSTSTORE_RESOURCE)
            ?: error("Truststore resource not found: $TRUSTSTORE_RESOURCE")
        trustStoreStream.use { stream -> trustStore.load(stream, KEYSTORE_PASSWORD.toCharArray()) }

        val trustManagerFactory = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm())
        trustManagerFactory.init(trustStore)

        val sslContext = SSLContext.getInstance("TLS")
        sslContext.init(null, trustManagerFactory.trustManagers, null)
        return sslContext.socketFactory
    }
}
