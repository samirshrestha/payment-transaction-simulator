package host.domain

/**
 * Real Account-store decisioning for the domain seam ([TransactionProcessor]). Authorization is
 * implemented here; Financial (#7) and Reversal (#8) land in later tickets.
 */
class AccountTransactionProcessor(private val accounts: AccountStore) : TransactionProcessor {

    override fun process(request: TransactionRequest): TransactionResponse = when (request.type) {
        TransactionType.AUTHORIZATION -> authorize(request)
        TransactionType.FINANCIAL, TransactionType.REVERSAL ->
            error("${request.type} is not yet implemented by AccountTransactionProcessor")
    }

    private fun authorize(request: TransactionRequest): TransactionResponse =
        when (val outcome = accounts.authorize(request.pan, request.amount)) {
            is AuthorizationOutcome.Approved ->
                TransactionResponse(type = request.type, stan = request.stan, approved = true)
            is AuthorizationOutcome.Declined ->
                TransactionResponse(
                    type = request.type,
                    stan = request.stan,
                    approved = false,
                    declineReason = outcome.reason,
                )
        }
}
