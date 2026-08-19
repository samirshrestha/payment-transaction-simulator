package host.domain

import kotlin.test.Test
import kotlin.test.assertEquals

class StubTransactionProcessorTest {

    private val processor = StubTransactionProcessor()

    @Test
    fun `approves an Authorization request and echoes its STAN`() {
        val request = TransactionRequest(
            type = TransactionType.AUTHORIZATION,
            stan = "000001",
            pan = "4111111111111111",
            amount = 12345L,
        )

        val response = processor.process(request)

        assertEquals(
            TransactionResponse(type = TransactionType.AUTHORIZATION, stan = "000001"),
            response,
        )
    }

    @Test
    fun `approves a Financial request and echoes its STAN`() {
        val request = TransactionRequest(
            type = TransactionType.FINANCIAL,
            stan = "000002",
            pan = "4111111111111111",
            amount = 500L,
        )

        val response = processor.process(request)

        assertEquals(
            TransactionResponse(type = TransactionType.FINANCIAL, stan = "000002"),
            response,
        )
    }

    @Test
    fun `approves a Reversal request and echoes its STAN`() {
        val request = TransactionRequest(
            type = TransactionType.REVERSAL,
            stan = "000003",
            pan = "4111111111111111",
            amount = 500L,
        )

        val response = processor.process(request)

        assertEquals(
            TransactionResponse(type = TransactionType.REVERSAL, stan = "000003"),
            response,
        )
    }
}
