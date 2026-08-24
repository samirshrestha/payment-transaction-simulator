package host.domain

/**
 * Real Account-store decisioning for the domain seam ([TransactionProcessor]). Authorization and
 * Financial are implemented here; Reversal (#8) lands in a later ticket.
 */
class AccountTransactionProcessor(private val accounts: AccountStore) : TransactionProcessor {

    override fun process(request: TransactionRequest): TransactionResponse = when (request.type) {
        TransactionType.AUTHORIZATION -> respond(request, accounts.authorize(request.pan, request.amount))
        TransactionType.FINANCIAL -> respond(request, accounts.financial(request.pan, request.amount))
        TransactionType.REVERSAL -> error("${request.type} is not yet implemented by AccountTransactionProcessor")
    }

    private fun respond(request: TransactionRequest, outcome: AccountOutcome): TransactionResponse =
        when (outcome) {
            is AccountOutcome.Approved ->
                TransactionResponse(type = request.type, stan = request.stan)
            is AccountOutcome.Declined ->
                TransactionResponse(type = request.type, stan = request.stan, declineReason = outcome.reason)
        }
}
