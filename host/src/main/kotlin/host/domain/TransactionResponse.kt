package host.domain

data class TransactionResponse(
    val type: TransactionType,
    val stan: String,
    val declineReason: DeclineReason? = null,
) {
    val approved: Boolean get() = declineReason == null
}
