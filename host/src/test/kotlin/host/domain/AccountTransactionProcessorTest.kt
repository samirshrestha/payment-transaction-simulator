package host.domain

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class AccountTransactionProcessorTest {

    private val knownPan = "4111111111111111"

    @Test
    fun `approves an Authorization within the limit and holds the amount against the limit, not the balance`() {
        val accounts = InMemoryAccountStore(listOf(Account(pan = knownPan, balance = 10_000L, limit = 5_000L)))
        val processor = AccountTransactionProcessor(accounts)
        val request = TransactionRequest(
            type = TransactionType.AUTHORIZATION,
            stan = "000001",
            pan = knownPan,
            amount = 2_000L,
        )

        val response = processor.process(request)

        assertEquals(
            TransactionResponse(type = TransactionType.AUTHORIZATION, stan = "000001"),
            response,
        )
        assertEquals(Account(pan = knownPan, balance = 10_000L, limit = 3_000L), accounts.find(knownPan))
    }

    @Test
    fun `declines an Authorization over the limit with Insufficient Funds`() {
        val accounts = InMemoryAccountStore(listOf(Account(pan = knownPan, balance = 10_000L, limit = 1_000L)))
        val processor = AccountTransactionProcessor(accounts)
        val request = TransactionRequest(
            type = TransactionType.AUTHORIZATION,
            stan = "000002",
            pan = knownPan,
            amount = 2_000L,
        )

        val response = processor.process(request)

        assertEquals(
            TransactionResponse(
                type = TransactionType.AUTHORIZATION,
                stan = "000002",
                declineReason = DeclineReason.INSUFFICIENT_FUNDS,
            ),
            response,
        )
        assertEquals(1_000L, accounts.find(knownPan)?.limit)
        assertEquals(10_000L, accounts.find(knownPan)?.balance)
    }

    @Test
    fun `declines an Authorization for an unknown PAN with Invalid Account`() {
        val accounts = InMemoryAccountStore()
        val processor = AccountTransactionProcessor(accounts)
        val request = TransactionRequest(
            type = TransactionType.AUTHORIZATION,
            stan = "000003",
            pan = "9999999999999999",
            amount = 500L,
        )

        val response = processor.process(request)

        assertEquals(
            TransactionResponse(
                type = TransactionType.AUTHORIZATION,
                stan = "000003",
                declineReason = DeclineReason.INVALID_ACCOUNT,
            ),
            response,
        )
    }

    @Test
    fun `an approved Authorization carries no decline reason`() {
        val accounts = InMemoryAccountStore(listOf(Account(pan = knownPan, balance = 10_000L, limit = 5_000L)))
        val processor = AccountTransactionProcessor(accounts)
        val request = TransactionRequest(
            type = TransactionType.AUTHORIZATION,
            stan = "000004",
            pan = knownPan,
            amount = 500L,
        )

        val response = processor.process(request)

        assertNull(response.declineReason)
    }
}
