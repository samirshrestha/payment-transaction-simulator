package host.domain

class InMemoryAccountStore(accounts: Collection<Account> = emptyList()) : AccountStore {

    private val accountsByPan = accounts.associateBy { it.pan }.toMutableMap()

    override fun find(pan: String): Account? = accountsByPan[pan]

    override fun authorize(pan: String, amount: Long): AuthorizationOutcome {
        require(amount >= 0) { "amount must be non-negative, was $amount" }

        val account = accountsByPan[pan]
            ?: return AuthorizationOutcome.Declined(DeclineReason.INVALID_ACCOUNT)

        if (amount > account.limit) {
            return AuthorizationOutcome.Declined(DeclineReason.INSUFFICIENT_FUNDS)
        }

        val held = account.copy(limit = account.limit - amount)
        accountsByPan[pan] = held
        return AuthorizationOutcome.Approved(held)
    }
}
