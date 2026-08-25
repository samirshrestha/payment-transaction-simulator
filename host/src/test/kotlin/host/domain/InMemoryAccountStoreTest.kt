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

    @Test
    fun `releaseAuthorization restores the amount to the limit, leaving the balance untouched`() {
        val store = InMemoryAccountStore(listOf(Account(pan = knownPan, balance = 10_000L, limit = 3_000L)))

        val account = store.releaseAuthorization(knownPan, 2_000L)

        assertEquals(Account(pan = knownPan, balance = 10_000L, limit = 5_000L), account)
        assertEquals(Account(pan = knownPan, balance = 10_000L, limit = 5_000L), store.find(knownPan))
    }

    @Test
    fun `releaseAuthorization on an unknown PAN fails instead of silently succeeding`() {
        val store = InMemoryAccountStore()

        assertFailsWith<IllegalArgumentException> { store.releaseAuthorization("9999999999999999", 500L) }
    }

    @Test
    fun `releaseAuthorization refuses a negative amount instead of silently decreasing the limit`() {
        val store = InMemoryAccountStore(listOf(Account(pan = knownPan, balance = 10_000L, limit = 3_000L)))

        assertFailsWith<IllegalArgumentException> { store.releaseAuthorization(knownPan, -500L) }
        assertEquals(Account(pan = knownPan, balance = 10_000L, limit = 3_000L), store.find(knownPan))
    }

    @Test
    fun `restoreBalance restores the amount to the balance, leaving the limit untouched`() {
        val store = InMemoryAccountStore(listOf(Account(pan = knownPan, balance = 8_000L, limit = 5_000L)))

        val account = store.restoreBalance(knownPan, 2_000L)

        assertEquals(Account(pan = knownPan, balance = 10_000L, limit = 5_000L), account)
        assertEquals(Account(pan = knownPan, balance = 10_000L, limit = 5_000L), store.find(knownPan))
    }

    @Test
    fun `restoreBalance on an unknown PAN fails instead of silently succeeding`() {
        val store = InMemoryAccountStore()

        assertFailsWith<IllegalArgumentException> { store.restoreBalance("9999999999999999", 500L) }
    }

    @Test
    fun `restoreBalance refuses a negative amount instead of silently decreasing the balance`() {
        val store = InMemoryAccountStore(listOf(Account(pan = knownPan, balance = 8_000L, limit = 5_000L)))

        assertFailsWith<IllegalArgumentException> { store.restoreBalance(knownPan, -500L) }
        assertEquals(Account(pan = knownPan, balance = 8_000L, limit = 5_000L), store.find(knownPan))
    }

    @Test
    fun `financialAdvice moves an amount directly against the balance, leaving the limit untouched`() {
        val store = InMemoryAccountStore(listOf(Account(pan = knownPan, balance = 10_000L, limit = 5_000L)))

        val account = store.financialAdvice(knownPan, 2_000L)

        assertEquals(Account(pan = knownPan, balance = 8_000L, limit = 5_000L), account)
        assertEquals(Account(pan = knownPan, balance = 8_000L, limit = 5_000L), store.find(knownPan))
    }

    @Test
    fun `financialAdvice applies an amount exceeding the balance, pushing it negative instead of declining`() {
        val store = InMemoryAccountStore(listOf(Account(pan = knownPan, balance = 1_000L, limit = 5_000L)))

        val account = store.financialAdvice(knownPan, 2_000L)

        assertEquals(Account(pan = knownPan, balance = -1_000L, limit = 5_000L), account)
        assertEquals(Account(pan = knownPan, balance = -1_000L, limit = 5_000L), store.find(knownPan))
    }

    @Test
    fun `financialAdvice on an unknown PAN fails instead of silently succeeding`() {
        val store = InMemoryAccountStore()

        assertFailsWith<IllegalArgumentException> { store.financialAdvice("9999999999999999", 500L) }
    }

    @Test
    fun `financialAdvice refuses a negative amount instead of silently increasing the balance`() {
        val store = InMemoryAccountStore(listOf(Account(pan = knownPan, balance = 1_000L, limit = 5_000L)))

        assertFailsWith<IllegalArgumentException> { store.financialAdvice(knownPan, -500L) }
        assertEquals(Account(pan = knownPan, balance = 1_000L, limit = 5_000L), store.find(knownPan))
    }
}
