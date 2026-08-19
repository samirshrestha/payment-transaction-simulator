package host.domain

import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class TransactionResponseTest {

    @Test
    fun `is approved when there is no decline reason`() {
        val response = TransactionResponse(type = TransactionType.AUTHORIZATION, stan = "000001")

        assertTrue(response.approved)
    }

    @Test
    fun `is not approved when a decline reason is present`() {
        val response = TransactionResponse(
            type = TransactionType.AUTHORIZATION,
            stan = "000001",
            declineReason = DeclineReason.INSUFFICIENT_FUNDS,
        )

        assertFalse(response.approved)
    }
}
