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

    @Test
    fun `a malformed frame on one connection doesn't stop the server from serving a later connection`() {
        val server = HostServer(port = 0, processor = StubTransactionProcessor())
        server.start()

        server.use {
            // Connection 1: a frame that's well-formed at the transport level (correct length
            // prefix) but has an MTI RequestCodec.decode() rejects -- "0110" is a response MTI,
            // not a valid request one, same fixture shape as RequestCodecTest's bad-MTI case.
            clientTlsSocketFactory().createSocket("localhost", server.boundPort).use { badConnection ->
                val output = DataOutputStream(badConnection.getOutputStream())

                val malformedMti = "0110".toByteArray(Charsets.US_ASCII)
                val bitmap = byteArrayOf(0x50, 0x20, 0, 0, 0, 0, 0, 0) // DE2, DE4, DE11 present
                val pan = "164111111111111111".toByteArray(Charsets.US_ASCII)
                val amount = "000000005000".toByteArray(Charsets.US_ASCII)
                val stan = "000007".toByteArray(Charsets.US_ASCII)
                val malformedRequest = malformedMti + bitmap + pan + amount + stan

                output.writeShort(malformedRequest.size)
                output.write(malformedRequest)
                output.flush()
            }

            // Connection 2: brand new socket, well-formed request. A short client-side timeout
            // means this fails fast with SocketTimeoutException instead of hanging forever if
            // the server's accept loop already died processing connection 1.
            val socket = clientTlsSocketFactory().createSocket("localhost", server.boundPort) as SSLSocket
            socket.soTimeout = 2000
            socket.use {
                val output = DataOutputStream(socket.getOutputStream())
                val input = DataInputStream(socket.getInputStream())

                val request = TransactionRequest(
                    type = TransactionType.AUTHORIZATION,
                    stan = "000008",
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
                assertEquals("000008", response.stan)
                assertTrue(response.approved)
            }
        }
    }

    @Test
    fun `a stalled connection doesn't stop the server from serving a later connection`() {
        val server = HostServer(port = 0, processor = StubTransactionProcessor(), connectionTimeoutMillis = 300)
        server.start()

        server.use {
            // Connection 1: completes the TLS handshake (the write forces it), then sends only
            // half the 2-byte length prefix and nothing more -- a genuine mid-frame stall, not a
            // clean disconnect (which would hit the EOFException path instead of this one).
            val stalledSocket = clientTlsSocketFactory().createSocket("localhost", server.boundPort) as SSLSocket
            DataOutputStream(stalledSocket.getOutputStream()).apply {
                write(0)
                flush()
            }

            // Connection 2: soTimeout comfortably longer than the server's 300ms, so this waits
            // out connection 1's timeout instead of racing it, but still fails fast if the server
            // never gets to connection 2 at all.
            val socket = clientTlsSocketFactory().createSocket("localhost", server.boundPort) as SSLSocket
            socket.soTimeout = 3000
            socket.use {
                val output = DataOutputStream(socket.getOutputStream())
                val input = DataInputStream(socket.getInputStream())

                val request = TransactionRequest(
                    type = TransactionType.AUTHORIZATION,
                    stan = "000009",
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
                assertEquals("000009", response.stan)
                assertTrue(response.approved)
            }

            stalledSocket.close()
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
