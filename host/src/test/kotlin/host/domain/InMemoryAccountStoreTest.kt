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

        val approved = assertIs<AccountOutcome.Approved>(outcome)
        assertEquals(Account(pan = knownPan, balance = 10_000L, limit = 3_000L), approved.account)
        assertEquals(Account(pan = knownPan, balance = 10_000L, limit = 3_000L), store.find(knownPan))
    }

    @Test
    fun `declines with Insufficient Funds when the amount exceeds the limit, leaving the account unchanged`() {
        val store = InMemoryAccountStore(listOf(Account(pan = knownPan, balance = 10_000L, limit = 1_000L)))

        val outcome = store.authorize(knownPan, 2_000L)

        val declined = assertIs<AccountOutcome.Declined>(outcome)
        assertEquals(DeclineReason.INSUFFICIENT_FUNDS, declined.reason)
        assertEquals(Account(pan = knownPan, balance = 10_000L, limit = 1_000L), store.find(knownPan))
    }

    @Test
    fun `declines with Invalid Account for an unknown PAN`() {
        val store = InMemoryAccountStore()

        val outcome = store.authorize("9999999999999999", 500L)

        val declined = assertIs<AccountOutcome.Declined>(outcome)
        assertEquals(DeclineReason.INVALID_ACCOUNT, declined.reason)
    }

    @Test
    fun `authorizing an amount exactly equal to the limit is approved`() {
        val store = InMemoryAccountStore(listOf(Account(pan = knownPan, balance = 10_000L, limit = 1_000L)))

        val outcome = store.authorize(knownPan, 1_000L)

        val approved = assertIs<AccountOutcome.Approved>(outcome)
        assertEquals(0L, approved.account.limit)
    }

    @Test
    fun `refuses a negative amount instead of treating it as satisfying the limit`() {
        val store = InMemoryAccountStore(listOf(Account(pan = knownPan, balance = 10_000L, limit = 1_000L)))

        assertFailsWith<IllegalArgumentException> { store.authorize(knownPan, -500L) }
        assertEquals(Account(pan = knownPan, balance = 10_000L, limit = 1_000L), store.find(knownPan))
    }

    @Test
    fun `moves an amount within the balance directly, leaving the limit untouched`() {
        val store = InMemoryAccountStore(listOf(Account(pan = knownPan, balance = 10_000L, limit = 5_000L)))

        val outcome = store.financial(knownPan, 2_000L)

        val approved = assertIs<AccountOutcome.Approved>(outcome)
        assertEquals(Account(pan = knownPan, balance = 8_000L, limit = 5_000L), approved.account)
        assertEquals(Account(pan = knownPan, balance = 8_000L, limit = 5_000L), store.find(knownPan))
    }

    @Test
    fun `declines a Financial with Insufficient Funds when the amount exceeds the balance, leaving the account unchanged`() {
        val store = InMemoryAccountStore(listOf(Account(pan = knownPan, balance = 1_000L, limit = 5_000L)))

        val outcome = store.financial(knownPan, 2_000L)

        val declined = assertIs<AccountOutcome.Declined>(outcome)
        assertEquals(DeclineReason.INSUFFICIENT_FUNDS, declined.reason)
        assertEquals(Account(pan = knownPan, balance = 1_000L, limit = 5_000L), store.find(knownPan))
    }

    @Test
    fun `declines a Financial with Invalid Account for an unknown PAN`() {
        val store = InMemoryAccountStore()

        val outcome = store.financial("9999999999999999", 500L)

        val declined = assertIs<AccountOutcome.Declined>(outcome)
        assertEquals(DeclineReason.INVALID_ACCOUNT, declined.reason)
    }

    @Test
    fun `moving an amount exactly equal to the balance is approved and leaves a zero balance`() {
        val store = InMemoryAccountStore(listOf(Account(pan = knownPan, balance = 1_000L, limit = 5_000L)))

        val outcome = store.financial(knownPan, 1_000L)

        val approved = assertIs<AccountOutcome.Approved>(outcome)
        assertEquals(0L, approved.account.balance)
    }

    @Test
    fun `refuses a negative Financial amount instead of treating it as satisfying the balance`() {
        val store = InMemoryAccountStore(listOf(Account(pan = knownPan, balance = 1_000L, limit = 5_000L)))

        assertFailsWith<IllegalArgumentException> { store.financial(knownPan, -500L) }
        assertEquals(Account(pan = knownPan, balance = 1_000L, limit = 5_000L), store.find(knownPan))
    }
}
