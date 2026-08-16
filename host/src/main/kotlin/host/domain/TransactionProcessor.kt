package host.domain

/**
 * Separates transaction decisioning from transport and wire codecs.
 *
 * Transport code submits a decoded request and receives a domain response.
 */
fun interface TransactionProcessor {
    fun process(request: TransactionRequest): TransactionResponse
}
