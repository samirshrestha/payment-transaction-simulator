package host.domain

data class TransactionResponse(
    val type: TransactionType,
    val stan: String,
    val approved: Boolean,
    val declineReason: DeclineReason? = null,
) {
    init {
        require(!approved || declineReason == null) { "An approved response cannot carry a decline reason" }
    }
}
