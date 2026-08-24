package host.domain

/** Real Account-store decisioning for the domain seam ([TransactionProcessor]). */
class AccountTransactionProcessor(private val accounts: AccountStore) : TransactionProcessor {

    /**
     * STAN -> what a Reversal targeting it must undo, per `host/CONTEXT.md`'s STAN entry: valid
     * indefinitely for the process lifetime. Only an approved Authorization or Financial is ever
     * recorded — a Reversal carries the target's STAN rather than one of its own, so it's never
     * recorded here itself, and this type has no variant for it.
     */
    private sealed interface Reversible {
        val pan: String
        val amount: Long

        data class Authorization(override val pan: String, override val amount: Long) : Reversible
        data class Financial(override val pan: String, override val amount: Long) : Reversible
    }

    private val reversibleByStan = mutableMapOf<String, Reversible>()

    override fun process(request: TransactionRequest): TransactionResponse = when (request.type) {
        TransactionType.AUTHORIZATION ->
            respond(request, accounts.authorize(request.pan, request.amount)) {
                Reversible.Authorization(request.pan, request.amount)
            }
        TransactionType.FINANCIAL ->
            respond(request, accounts.financial(request.pan, request.amount)) {
                Reversible.Financial(request.pan, request.amount)
            }
        TransactionType.REVERSAL -> reverse(request)
    }

    private fun respond(
        request: TransactionRequest,
        outcome: AccountOutcome,
        reversible: () -> Reversible,
    ): TransactionResponse = when (outcome) {
        is AccountOutcome.Approved -> {
            reversibleByStan[request.stan] = reversible()
            TransactionResponse(type = request.type, stan = request.stan)
        }
        is AccountOutcome.Declined ->
            TransactionResponse(type = request.type, stan = request.stan, declineReason = outcome.reason)
    }

    /** Reversal never declines: it undoes whatever the target STAN's own request did. */
    private fun reverse(request: TransactionRequest): TransactionResponse {
        val target = requireNotNull(reversibleByStan[request.stan]) {
            "Reversal references unknown STAN '${request.stan}'"
        }
        when (target) {
            is Reversible.Authorization -> accounts.releaseAuthorization(target.pan, target.amount)
            is Reversible.Financial -> accounts.restoreBalance(target.pan, target.amount)
        }
        return TransactionResponse(type = TransactionType.REVERSAL, stan = request.stan)
    }
}
