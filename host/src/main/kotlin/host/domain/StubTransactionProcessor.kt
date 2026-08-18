package host.domain

class StubTransactionProcessor : TransactionProcessor {
    override fun process(request: TransactionRequest) =
        TransactionResponse(type = request.type, stan = request.stan, approved = true)
}
