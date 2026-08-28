package host

import host.domain.AccountTransactionProcessor
import host.domain.DeclineReason
import host.domain.InMemoryAccountStore
import host.domain.TransactionRequest
import host.domain.TransactionType
import host.transport.DevTls
import host.transport.HostServer
import host.wire.RequestCodec
import host.wire.ResponseCodec
import java.io.DataInputStream
import java.io.DataOutputStream
import java.security.KeyStore
import javax.net.ssl.SSLContext
import javax.net.ssl.SSLSocket
import javax.net.ssl.SSLSocketFactory
import javax.net.ssl.TrustManagerFactory
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * Exercises the real wiring [Main.kt][main] uses in production -- [AccountTransactionProcessor]
 * backed by an [InMemoryAccountStore] seeded with [DEMO_ACCOUNTS] -- over a real TLS socket, to
 * confirm the running server is live end-to-end (not the [StubTransactionProcessor][host.domain.StubTransactionProcessor]
 * `HostServerTest` uses to isolate transport concerns).
 */
class MainIntegrationTest {

    @Test
    fun `a demo Account with enough balance approves a Financial`() {
        val server = HostServer(port = 0, processor = AccountTransactionProcessor(InMemoryAccountStore(DEMO_ACCOUNTS)))
        server.start()

        server.use {
            val response = send(
                server,
                TransactionRequest(type = TransactionType.FINANCIAL, stan = "000001", pan = "4111111111111111", amount = 10_000L),
            )

            assertTrue(response.approved)
        }
    }

    @Test
    fun `a demo Account without enough balance declines a Financial as Insufficient Funds`() {
        val server = HostServer(port = 0, processor = AccountTransactionProcessor(InMemoryAccountStore(DEMO_ACCOUNTS)))
        server.start()

        server.use {
            val response = send(
                server,
                TransactionRequest(type = TransactionType.FINANCIAL, stan = "000002", pan = "5500000000000004", amount = 1_000L),
            )

            assertEquals(DeclineReason.INSUFFICIENT_FUNDS, response.declineReason)
        }
    }

    private fun send(server: HostServer, request: TransactionRequest) =
        (clientTlsSocketFactory().createSocket("localhost", server.boundPort) as SSLSocket).use { socket ->
            val output = DataOutputStream(socket.getOutputStream())
            val input = DataInputStream(socket.getInputStream())

            val requestBytes = RequestCodec.encode(request)
            output.writeShort(requestBytes.size)
            output.write(requestBytes)
            output.flush()

            val responseLength = input.readUnsignedShort()
            val responseBytes = ByteArray(responseLength)
            input.readFully(responseBytes)
            ResponseCodec.decode(responseBytes)
        }

    private fun clientTlsSocketFactory(): SSLSocketFactory {
        val trustStore = KeyStore.getInstance("PKCS12")
        val trustStoreStream = MainIntegrationTest::class.java.getResourceAsStream("/tls/host-truststore.p12")
            ?: error("Test truststore resource not found")
        trustStoreStream.use { stream -> trustStore.load(stream, DevTls.KEYSTORE_PASSWORD.toCharArray()) }

        val trustManagerFactory = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm())
        trustManagerFactory.init(trustStore)

        val sslContext = SSLContext.getInstance("TLS")
        sslContext.init(null, trustManagerFactory.trustManagers, null)
        return sslContext.socketFactory
    }
}
