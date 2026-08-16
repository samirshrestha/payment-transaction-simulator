package host

import host.domain.StubTransactionProcessor
import host.transport.HostServer

private const val DEFAULT_PORT = 8583

fun main() {
    val server = HostServer(port = DEFAULT_PORT, processor = StubTransactionProcessor())
    Runtime.getRuntime().addShutdownHook(Thread { server.close() })

    server.start()
    println("Host listening on port ${server.boundPort} (TLS)")

    Thread.sleep(Long.MAX_VALUE)
}
