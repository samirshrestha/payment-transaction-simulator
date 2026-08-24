package host.domain

/** PAN-keyed Account store backing Host's approve/decline decisioning, per ADR-0001. */
interface AccountStore {
    fun find(pan: String): Account?

    /** Holds `amount` against the Account's limit; never moves the balance. */
    fun authorize(pan: String, amount: Long): AccountOutcome

    /** Moves `amount` directly against the Account's balance; never touches the limit. */
    fun financial(pan: String, amount: Long): AccountOutcome
}

/** Outcome of an Account-store decision, shared by Authorization and Financial. */
sealed interface AccountOutcome {
    data class Approved(val account: Account) : AccountOutcome
    data class Declined(val reason: DeclineReason) : AccountOutcome
}
