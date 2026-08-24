package host.domain

class InMemoryAccountStore(accounts: Collection<Account> = emptyList()) : AccountStore {

    private val accountsByPan = accounts.associateBy { it.pan }.toMutableMap()

    override fun find(pan: String): Account? = accountsByPan[pan]

    override fun authorize(pan: String, amount: Long): AccountOutcome =
        decide(pan, amount, available = Account::limit) { account, held ->
            account.copy(limit = account.limit - held)
        }

    override fun financial(pan: String, amount: Long): AccountOutcome =
        decide(pan, amount, available = Account::balance) { account, moved ->
            account.copy(balance = account.balance - moved)
        }

    override fun releaseAuthorization(pan: String, amount: Long): Account =
        reverse(pan, amount) { it.copy(limit = it.limit + amount) }

    override fun restoreBalance(pan: String, amount: Long): Account =
        reverse(pan, amount) { it.copy(balance = it.balance + amount) }

    /**
     * Reversal never declines: the PAN is only ever reached here via a STAN that a prior
     * [authorize]/[financial] call already proved valid, so an unknown PAN would be an invariant
     * violation, not a domain outcome.
     */
    private fun reverse(pan: String, amount: Long, apply: (Account) -> Account): Account {
        require(amount >= 0) { "amount must be non-negative, was $amount" }

        val account = requireNotNull(accountsByPan[pan]) { "Cannot reverse unknown PAN $pan" }
        val updated = apply(account)
        accountsByPan[pan] = updated
        return updated
    }

    /**
     * Shared Account-store decisioning: unknown PAN declines Invalid Account, `amount` exceeding
     * `available` declines Insufficient Funds, otherwise `apply` commits the mutation.
     */
    private inline fun decide(
        pan: String,
        amount: Long,
        available: (Account) -> Long,
        apply: (Account, Long) -> Account,
    ): AccountOutcome {
        require(amount >= 0) { "amount must be non-negative, was $amount" }

        val account = accountsByPan[pan]
            ?: return AccountOutcome.Declined(DeclineReason.INVALID_ACCOUNT)

        if (amount > available(account)) {
            return AccountOutcome.Declined(DeclineReason.INSUFFICIENT_FUNDS)
        }

        val updated = apply(account, amount)
        accountsByPan[pan] = updated
        return AccountOutcome.Approved(updated)
    }
}
