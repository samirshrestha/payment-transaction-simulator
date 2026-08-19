package host.domain

/** PAN-keyed Account store backing Host's approve/decline decisioning, per ADR-0001. */
interface AccountStore {
    fun find(pan: String): Account?

    /** Holds `amount` against the Account's limit; never moves the balance. */
    fun authorize(pan: String, amount: Long): AuthorizationOutcome
}

sealed interface AuthorizationOutcome {
    data class Approved(val account: Account) : AuthorizationOutcome
    data class Declined(val reason: DeclineReason) : AuthorizationOutcome
}
