package host.domain

data class TransactionRequest(
    val type: TransactionType,
    val stan: String,
    val pan: String,
    val amount: Long,
)
