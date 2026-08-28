package host.domain

/** Real Account-store decisioning for the domain seam ([TransactionProcessor]). */
class AccountTransactionProcessor(private val accounts: AccountStore) : TransactionProcessor {

    /**
     * STAN -> what a Reversal targeting it must undo, per `host/CONTEXT.md`'s STAN entry: valid
     * indefinitely for the process lifetime. Only an approved Authorization or online Financial
     * is ever recorded — a Financial Advice isn't a valid Reversal target at all (per ADR-0004,
     * Terminal recovers a lost Advice ack by repeating the same Advice, not by reversing it), and
     * a Reversal carries the target's STAN rather than one of its own, so it's never recorded
     * here itself — neither has a variant here.
     */
    private sealed interface Reversible {
        val pan: String
        val amount: Long

        data class Authorization(override val pan: String, override val amount: Long) : Reversible
        data class Financial(override val pan: String, override val amount: Long) : Reversible
    }

    private val reversibleByStan = mutableMapOf<String, Reversible>()

    /** STANs a Reversal has already been applied against -- guards a repeated Reversal from releasing/restoring twice. */
    private val reversedStans = mutableSetOf<String>()

    /** STAN -> what a Financial Advice applied, so a repeated Advice can be re-acknowledged without re-applying it. */
    private data class AdviceRecord(val pan: String, val amount: Long)
    private val advicedByStan = mutableMapOf<String, AdviceRecord>()

    override fun process(request: TransactionRequest): TransactionResponse {
        requireAdviceIsFinancial(request.type, request.advice)
        requireRepeatIsReversalOrAdvice(request.type, request.advice, request.repeat)
        return when (request.type) {
            TransactionType.AUTHORIZATION ->
                respond(request, accounts.authorize(request.pan, request.amount)) {
                    Reversible.Authorization(request.pan, request.amount)
                }
            TransactionType.FINANCIAL ->
                if (request.advice) advise(request)
                else respond(request, accounts.financial(request.pan, request.amount)) {
                    Reversible.Financial(request.pan, request.amount)
                }
            TransactionType.REVERSAL -> reverse(request)
        }
    }

    /**
     * Financial Advice never declines, per `host/CONTEXT.md`'s Financial Advice entry — it just
     * applies the Account change. A repeated Advice (same STAN, Repeat Indicator set) is
     * re-acknowledged without applying the Account change a second time.
     */
    private fun advise(request: TransactionRequest): TransactionResponse {
        val previous = advicedByStan[request.stan]
        if (previous != null) {
            require(request.repeat) {
                "Financial Advice for STAN '${request.stan}' was already processed; a retry must set the Repeat Indicator"
            }
            require(request.pan == previous.pan && request.amount == previous.amount) {
                "Repeated Financial Advice for STAN '${request.stan}' does not match the recorded transaction " +
                    "(expected pan=${previous.pan} amount=${previous.amount}, got pan=${request.pan} amount=${request.amount})"
            }
            return TransactionResponse(type = request.type, stan = request.stan, advice = true)
        }
        accounts.financialAdvice(request.pan, request.amount)
        advicedByStan[request.stan] = AdviceRecord(request.pan, request.amount)
        return TransactionResponse(type = request.type, stan = request.stan, advice = true)
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

    /**
     * Reversal never declines: it undoes whatever the target STAN's own request did. Per ISO
     * 8583, DE2/DE4 on the Reversal message must match the original transaction Host recorded
     * for that STAN — a mismatch is an invariant violation, not a domain outcome. A repeated
     * Reversal (same STAN, Repeat Indicator set) is re-acknowledged without releasing/restoring
     * the Account a second time.
     */
    private fun reverse(request: TransactionRequest): TransactionResponse {
        val target = requireNotNull(reversibleByStan[request.stan]) {
            "Reversal references unknown STAN '${request.stan}'"
        }
        require(request.pan == target.pan && request.amount == target.amount) {
            "Reversal for STAN '${request.stan}' does not match the recorded transaction " +
                "(expected pan=${target.pan} amount=${target.amount}, got pan=${request.pan} amount=${request.amount})"
        }
        if (request.stan in reversedStans) {
            require(request.repeat) {
                "Reversal for STAN '${request.stan}' was already processed; a retry must set the Repeat Indicator"
            }
            return TransactionResponse(type = TransactionType.REVERSAL, stan = request.stan)
        }
        when (target) {
            is Reversible.Authorization -> accounts.releaseAuthorization(target.pan, target.amount)
            is Reversible.Financial -> accounts.restoreBalance(target.pan, target.amount)
        }
        reversedStans += request.stan
        return TransactionResponse(type = TransactionType.REVERSAL, stan = request.stan)
    }
}
