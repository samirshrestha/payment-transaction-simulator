package host.domain

/** A simulated Host record per PAN, per `host/CONTEXT.md`'s Account entry. */
data class Account(
    val pan: String,
    val balance: Long,
    val limit: Long,
)
