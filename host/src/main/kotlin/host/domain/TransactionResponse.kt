package host.domain

data class TransactionResponse(
    val type: TransactionType,
    val stan: String,
    val declineReason: DeclineReason? = null,
    /** True when this acknowledges a Financial Advice rather than an online request. */
    val advice: Boolean = false,
) {
    val approved: Boolean get() = declineReason == null
}
