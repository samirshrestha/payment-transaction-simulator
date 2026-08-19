package host.domain

import kotlin.test.Test
import kotlin.test.assertFailsWith

class TransactionResponseTest {

    @Test
    fun `refuses an approved response that carries a decline reason`() {
        assertFailsWith<IllegalArgumentException> {
            TransactionResponse(
                type = TransactionType.AUTHORIZATION,
                stan = "000001",
                approved = true,
                declineReason = DeclineReason.INSUFFICIENT_FUNDS,
            )
        }
    }
}
