package host.wire

import host.domain.TransactionType

/** ISO 8583 Message Type Indicator: version + class + function + origin digits. */
internal object Mti {
    private const val VERSION_DIGIT = '0'
    private const val REQUEST_FUNCTION_DIGIT = '0'
    private const val RESPONSE_FUNCTION_DIGIT = '1'
    private const val ORIGINAL_ORIGIN_DIGIT = '0'

    fun request(type: TransactionType): String =
        "$VERSION_DIGIT${classDigitFor(type)}$REQUEST_FUNCTION_DIGIT$ORIGINAL_ORIGIN_DIGIT"

    fun response(type: TransactionType): String =
        "$VERSION_DIGIT${classDigitFor(type)}$RESPONSE_FUNCTION_DIGIT$ORIGINAL_ORIGIN_DIGIT"

    fun transactionType(mti: String): TransactionType = when (mti[1]) {
        '1' -> TransactionType.AUTHORIZATION
        '2' -> TransactionType.FINANCIAL
        '4' -> TransactionType.REVERSAL
        else -> throw IllegalArgumentException("Unrecognized MTI class digit '${mti[1]}' in MTI '$mti'")
    }

    private fun classDigitFor(type: TransactionType): Char = when (type) {
        TransactionType.AUTHORIZATION -> '1'
        TransactionType.FINANCIAL -> '2'
        TransactionType.REVERSAL -> '4'
    }
}
