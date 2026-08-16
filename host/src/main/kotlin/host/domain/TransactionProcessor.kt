package host.domain

fun interface TransactionProcessor {
    fun process(request: TransactionRequest): TransactionResponse
}
