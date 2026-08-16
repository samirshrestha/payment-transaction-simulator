package host.transport

import host.domain.TransactionProcessor
import host.wire.RequestCodec
import host.wire.ResponseCodec
import java.io.DataInputStream
import java.io.DataOutputStream
import java.io.EOFException
import java.net.Socket
import java.security.KeyStore
import javax.net.ssl.KeyManagerFactory
import javax.net.ssl.SSLContext
import javax.net.ssl.SSLServerSocket

/**
 * TCP socket server, one-way TLS, decoding each request onto the wire and delegating to the
 * domain seam ([TransactionProcessor]) per ADR-0005. Host processes one connection's messages
 * synchronously and sequentially, per `host/CONTEXT.md`'s single-threaded processing decision.
 */
class HostServer(
    port: Int,
    private val processor: TransactionProcessor,
    keystoreResource: String = DevTls.KEYSTORE_RESOURCE,
    keystorePassword: String = DevTls.KEYSTORE_PASSWORD,
) : AutoCloseable {

    private val serverSocket =
        buildSslContext(keystoreResource, keystorePassword).serverSocketFactory
            .createServerSocket(port) as SSLServerSocket

    private val acceptThread = Thread(::acceptLoop, "host-server-accept").apply { isDaemon = true }

    val boundPort: Int get() = serverSocket.localPort

    fun start() {
        acceptThread.start()
    }

    override fun close() {
        serverSocket.close()
    }

    private fun acceptLoop() {
        while (!serverSocket.isClosed) {
            val client = try {
                serverSocket.accept()
            } catch (e: Exception) {
                break
            }
            handleConnection(client)
        }
    }

    private fun handleConnection(socket: Socket) {
        socket.use {
            val input = DataInputStream(it.getInputStream())
            val output = DataOutputStream(it.getOutputStream())
            try {
                while (true) {
                    val requestLength = input.readUnsignedShort()
                    val requestBytes = ByteArray(requestLength)
                    input.readFully(requestBytes)

                    val request = RequestCodec.decode(requestBytes)
                    val response = processor.process(request)
                    val responseBytes = ResponseCodec.encode(response)

                    output.writeShort(responseBytes.size)
                    output.write(responseBytes)
                    output.flush()
                }
            } catch (e: EOFException) {
                // client closed the connection
            }
        }
    }

    companion object {
        private fun buildSslContext(keystoreResource: String, keystorePassword: String): SSLContext {
            val keyStore = KeyStore.getInstance("PKCS12")
            val keystoreStream = HostServer::class.java.getResourceAsStream(keystoreResource)
                ?: error("Keystore resource not found: $keystoreResource")
            keystoreStream.use { stream -> keyStore.load(stream, keystorePassword.toCharArray()) }

            val keyManagerFactory = KeyManagerFactory.getInstance(KeyManagerFactory.getDefaultAlgorithm())
            keyManagerFactory.init(keyStore, keystorePassword.toCharArray())

            return SSLContext.getInstance("TLS").apply {
                init(keyManagerFactory.keyManagers, null, null)
            }
        }
    }
}
