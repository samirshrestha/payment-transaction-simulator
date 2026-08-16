package host.domain

data class TransactionResponse(
    val type: TransactionType,
    val stan: String,
    val approved: Boolean,
)
