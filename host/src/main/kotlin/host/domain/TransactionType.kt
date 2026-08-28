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

/**
 * The Repeat Indicator (see `host/CONTEXT.md`) only marks a retried Reversal or Financial
 * Advice -- an online Authorization or Financial request has no retry-by-STAN concept.
 */
fun requireRepeatIsReversalOrAdvice(type: TransactionType, advice: Boolean, repeat: Boolean) {
    require(!repeat || type == TransactionType.REVERSAL || advice) {
        "Repeat Indicator is only valid for a Reversal or a Financial Advice, got type=$type advice=$advice"
    }
}
