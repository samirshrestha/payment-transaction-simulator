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
)
