package host.domain

enum class TransactionType {
    AUTHORIZATION,
    FINANCIAL,
    REVERSAL,
}

/** Financial Advice (see `host/CONTEXT.md`) is only defined for [TransactionType.FINANCIAL]. */
fun requireAdviceIsFinancial(type: TransactionType, advice: Boolean) {
    require(!advice || type == TransactionType.FINANCIAL) {
        "Advice is only defined for a Financial transaction, got $type"
    }
}
