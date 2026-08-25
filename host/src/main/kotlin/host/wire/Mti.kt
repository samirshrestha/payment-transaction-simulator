package host.wire

import host.domain.TransactionType
import host.domain.requireAdviceIsFinancial

/** ISO 8583 Message Type Indicator: version + class + function + origin digits. */
internal object Mti {
    private const val VERSION_DIGIT = '0'
    private const val REQUEST_FUNCTION_DIGIT = '0'
    private const val RESPONSE_FUNCTION_DIGIT = '1'
    private const val ADVICE_FUNCTION_DIGIT = '2'
    private const val ADVICE_RESPONSE_FUNCTION_DIGIT = '3'
    private const val ORIGINAL_ORIGIN_DIGIT = '0'

    /**
     * `advice` selects the function digit for a Financial Advice (2) instead of an online
     * request (0) -- same class digit as [TransactionType.FINANCIAL], per the published standard,
     * rather than a type of its own.
     */
    fun request(type: TransactionType, advice: Boolean = false): String {
        requireAdviceIsFinancial(type, advice)
        val functionDigit = if (advice) ADVICE_FUNCTION_DIGIT else REQUEST_FUNCTION_DIGIT
        return "$VERSION_DIGIT${classDigitFor(type)}$functionDigit$ORIGINAL_ORIGIN_DIGIT"
    }

    fun response(type: TransactionType, advice: Boolean = false): String {
        requireAdviceIsFinancial(type, advice)
        val functionDigit = if (advice) ADVICE_RESPONSE_FUNCTION_DIGIT else RESPONSE_FUNCTION_DIGIT
        return "$VERSION_DIGIT${classDigitFor(type)}$functionDigit$ORIGINAL_ORIGIN_DIGIT"
    }

    fun transactionType(mti: String): TransactionType = when (mti[1]) {
        '1' -> TransactionType.AUTHORIZATION
        '2' -> TransactionType.FINANCIAL
        '4' -> TransactionType.REVERSAL
        else -> throw IllegalArgumentException("Unrecognized MTI class digit '${mti[1]}' in MTI '$mti'")
    }

    /** True when the MTI's function digit marks a Financial Advice (2) or its acknowledgement (3). */
    fun isAdvice(mti: String): Boolean = mti[2] == ADVICE_FUNCTION_DIGIT || mti[2] == ADVICE_RESPONSE_FUNCTION_DIGIT

    private fun classDigitFor(type: TransactionType): Char = when (type) {
        TransactionType.AUTHORIZATION -> '1'
        TransactionType.FINANCIAL -> '2'
        TransactionType.REVERSAL -> '4'
    }
}
