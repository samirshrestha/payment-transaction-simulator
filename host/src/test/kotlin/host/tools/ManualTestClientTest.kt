package host.tools

import host.domain.DeclineReason
import host.domain.TransactionResponse
import host.domain.TransactionType
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertIs

class ManualTestClientTest {

    @Test
    fun `parses a well-formed Financial command`() {
        val command = parseCommand("FINANCIAL 4111111111111111 10000 1")

        val transaction = assertIs<Command.Transaction>(command)
        assertEquals(TransactionType.FINANCIAL, transaction.type)
        assertEquals("4111111111111111", transaction.pan)
        assertEquals(10000L, transaction.amount)
        assertEquals("1", transaction.stan)
        assertEquals(false, transaction.advice)
        assertEquals(false, transaction.repeat)
    }

    @Test
    fun `is case-insensitive on type and tolerant of extra whitespace`() {
        val command = parseCommand("  financial   4111111111111111   10000   1  ")

        val transaction = assertIs<Command.Transaction>(command)
        assertEquals(TransactionType.FINANCIAL, transaction.type)
    }

    @Test
    fun `parses an optional ADVICE flag`() {
        val command = parseCommand("FINANCIAL 4111111111111111 10000 1 ADVICE")

        val transaction = assertIs<Command.Transaction>(command)
        assertEquals(true, transaction.advice)
        assertEquals(false, transaction.repeat)
    }

    @Test
    fun `parses an optional REPEAT flag`() {
        val command = parseCommand("REVERSAL 4111111111111111 10000 1 REPEAT")

        val transaction = assertIs<Command.Transaction>(command)
        assertEquals(false, transaction.advice)
        assertEquals(true, transaction.repeat)
    }

    @Test
    fun `parses ADVICE and REPEAT together, for a retried Financial Advice, regardless of order`() {
        val transaction1 = assertIs<Command.Transaction>(parseCommand("FINANCIAL 4111111111111111 10000 1 ADVICE REPEAT"))
        assertEquals(true, transaction1.advice)
        assertEquals(true, transaction1.repeat)

        val transaction2 = assertIs<Command.Transaction>(parseCommand("FINANCIAL 4111111111111111 10000 1 REPEAT ADVICE"))
        assertEquals(true, transaction2.advice)
        assertEquals(true, transaction2.repeat)
    }

    @Test
    fun `rejects a duplicated flag`() {
        assertFailsWith<IllegalArgumentException> { parseCommand("FINANCIAL 4111111111111111 10000 1 ADVICE ADVICE") }
    }

    @Test
    fun `parses quit case-insensitively`() {
        assertIs<Command.Quit>(parseCommand("quit"))
        assertIs<Command.Quit>(parseCommand("QUIT"))
    }

    @Test
    fun `rejects an unknown transaction type`() {
        assertFailsWith<IllegalArgumentException> { parseCommand("PURCHASE 4111111111111111 10000 1") }
    }

    @Test
    fun `rejects a non-numeric amount`() {
        assertFailsWith<IllegalArgumentException> { parseCommand("FINANCIAL 4111111111111111 not-a-number 1") }
    }

    @Test
    fun `rejects an unknown trailing flag`() {
        assertFailsWith<IllegalArgumentException> { parseCommand("FINANCIAL 4111111111111111 10000 1 BOGUS") }
    }

    @Test
    fun `rejects too few tokens`() {
        assertFailsWith<IllegalArgumentException> { parseCommand("FINANCIAL 4111111111111111 10000") }
    }

    @Test
    fun `rejects an empty line`() {
        assertFailsWith<IllegalArgumentException> { parseCommand("   ") }
    }

    @Test
    fun `describes an approved response`() {
        val response = TransactionResponse(type = TransactionType.FINANCIAL, stan = "1")

        assertEquals("APPROVED", describe(response))
    }

    @Test
    fun `describes a declined response with its reason`() {
        val response = TransactionResponse(
            type = TransactionType.FINANCIAL,
            stan = "1",
            declineReason = DeclineReason.INSUFFICIENT_FUNDS,
        )

        assertEquals("DECLINED (INSUFFICIENT_FUNDS)", describe(response))
    }

    @Test
    fun `describes a Financial Advice acknowledgement`() {
        val response = TransactionResponse(type = TransactionType.FINANCIAL, stan = "1", advice = true)

        assertEquals("ACKNOWLEDGED (advice)", describe(response))
    }

    @Test
    fun `a decline reason takes priority over the advice flag if a response were ever to carry both`() {
        val response = TransactionResponse(
            type = TransactionType.FINANCIAL,
            stan = "1",
            declineReason = DeclineReason.INVALID_ACCOUNT,
            advice = true,
        )

        assertEquals("DECLINED (INVALID_ACCOUNT)", describe(response))
    }
}
