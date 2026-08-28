package host

import host.domain.Account
import host.domain.AccountTransactionProcessor
import host.domain.InMemoryAccountStore
import host.transport.HostServer

private const val DEFAULT_PORT = 8583

/**
 * Demo Accounts for manual/Terminal testing -- keep in sync with README's "Running the Host
 * simulator" section and `host/CONTEXT.md`. Amounts are minor units (cents), per DE4.
 */
internal val DEMO_ACCOUNTS = listOf(
    Account(pan = "4111111111111111", balance = 100_000L, limit = 50_000L),
    Account(pan = "5500000000000004", balance = 500L, limit = 500L),
)

fun main() {
    val processor = AccountTransactionProcessor(InMemoryAccountStore(DEMO_ACCOUNTS))
    val server = HostServer(port = DEFAULT_PORT, processor = processor)
    Runtime.getRuntime().addShutdownHook(Thread { server.close() })

    server.start()
    println("Host listening on port ${server.boundPort} (TLS)")

    Thread.sleep(Long.MAX_VALUE)
}
