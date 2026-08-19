package host.domain

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertIs

class InMemoryAccountStoreTest {

    private val knownPan = "4111111111111111"

    @Test
    fun `authorizes an amount within the limit, holding it against the limit not the balance`() {
        val store = InMemoryAccountStore(listOf(Account(pan = knownPan, balance = 10_000L, limit = 5_000L)))

        val outcome = store.authorize(knownPan, 2_000L)

        val approved = assertIs<AuthorizationOutcome.Approved>(outcome)
        assertEquals(Account(pan = knownPan, balance = 10_000L, limit = 3_000L), approved.account)
        assertEquals(Account(pan = knownPan, balance = 10_000L, limit = 3_000L), store.find(knownPan))
    }

    @Test
    fun `declines with Insufficient Funds when the amount exceeds the limit, leaving the account unchanged`() {
        val store = InMemoryAccountStore(listOf(Account(pan = knownPan, balance = 10_000L, limit = 1_000L)))

        val outcome = store.authorize(knownPan, 2_000L)

        val declined = assertIs<AuthorizationOutcome.Declined>(outcome)
        assertEquals(DeclineReason.INSUFFICIENT_FUNDS, declined.reason)
        assertEquals(Account(pan = knownPan, balance = 10_000L, limit = 1_000L), store.find(knownPan))
    }

    @Test
    fun `declines with Invalid Account for an unknown PAN`() {
        val store = InMemoryAccountStore()

        val outcome = store.authorize("9999999999999999", 500L)

        val declined = assertIs<AuthorizationOutcome.Declined>(outcome)
        assertEquals(DeclineReason.INVALID_ACCOUNT, declined.reason)
    }

    @Test
    fun `authorizing an amount exactly equal to the limit is approved`() {
        val store = InMemoryAccountStore(listOf(Account(pan = knownPan, balance = 10_000L, limit = 1_000L)))

        val outcome = store.authorize(knownPan, 1_000L)

        val approved = assertIs<AuthorizationOutcome.Approved>(outcome)
        assertEquals(0L, approved.account.limit)
    }

    @Test
    fun `refuses a negative amount instead of treating it as satisfying the limit`() {
        val store = InMemoryAccountStore(listOf(Account(pan = knownPan, balance = 10_000L, limit = 1_000L)))

        assertFailsWith<IllegalArgumentException> { store.authorize(knownPan, -500L) }
        assertEquals(Account(pan = knownPan, balance = 10_000L, limit = 1_000L), store.find(knownPan))
    }
}
