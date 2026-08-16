package host.transport

import host.domain.StubTransactionProcessor
import host.domain.TransactionRequest
import host.domain.TransactionType
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

class HostServerTest {

    @Test
    fun `sends a well-formed Authorization request over a real TLS socket and gets a real response back`() {
        val server = HostServer(port = 0, processor = StubTransactionProcessor())
        server.start()

        server.use {
            val socket = clientTlsSocketFactory().createSocket("localhost", server.boundPort) as SSLSocket
            socket.use {
                val output = DataOutputStream(socket.getOutputStream())
                val input = DataInputStream(socket.getInputStream())

                val request = TransactionRequest(
                    type = TransactionType.AUTHORIZATION,
                    stan = "000007",
                    pan = "4111111111111111",
                    amount = 5000L,
                )
                val requestBytes = RequestCodec.encode(request)
                output.writeShort(requestBytes.size)
                output.write(requestBytes)
                output.flush()

                val responseLength = input.readUnsignedShort()
                val responseBytes = ByteArray(responseLength)
                input.readFully(responseBytes)
                val response = ResponseCodec.decode(responseBytes)

                assertEquals(TransactionType.AUTHORIZATION, response.type)
                assertEquals("000007", response.stan)
                assertTrue(response.approved)
            }
        }
    }

    private fun clientTlsSocketFactory(): SSLSocketFactory {
        val trustStore = KeyStore.getInstance("PKCS12")
        val trustStoreStream = HostServerTest::class.java.getResourceAsStream("/tls/host-truststore.p12")
            ?: error("Test truststore resource not found")
        trustStoreStream.use { stream -> trustStore.load(stream, DevTls.KEYSTORE_PASSWORD.toCharArray()) }

        val trustManagerFactory = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm())
        trustManagerFactory.init(trustStore)

        val sslContext = SSLContext.getInstance("TLS")
        sslContext.init(null, trustManagerFactory.trustManagers, null)
        return sslContext.socketFactory
    }
}
