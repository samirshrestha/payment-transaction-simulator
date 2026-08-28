package host.domain

data class TransactionRequest(
    val type: TransactionType,
    val stan: String,
    val pan: String,
    val amount: Long,
    /**
     * True when this is a Financial Advice — Terminal reporting a Financial already approved
     * offline — rather than the online request/response pair. Only defined for [TransactionType.FINANCIAL].
     */
    val advice: Boolean = false,
    /**
     * The ISO 8583 MTI's origin digit: true when Terminal is retrying a Reversal or Financial
     * Advice it already sent under this same STAN (see `host/CONTEXT.md`'s Repeat Indicator
     * entry). Only defined for [TransactionType.REVERSAL] or a Financial [advice].
     */
    val repeat: Boolean = false,
)
