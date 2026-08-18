package host.wire

import host.domain.TransactionRequest
import java.nio.charset.StandardCharsets.US_ASCII

/** Translates between wire bytes and [TransactionRequest]: MTI + DE2 (PAN, LLVAR) + DE4 (amount) + DE11 (STAN). */
object RequestCodec {
    private const val DE_2_PAN = 2
    private const val DE_4_AMOUNT = 4
    private const val DE_11_STAN = 11

    private const val MTI_LENGTH = 4
    private const val BITMAP_LENGTH = 8
    private const val PAN_LENGTH_PREFIX_LENGTH = 2
    private const val PAN_MAX_LENGTH = 99 // largest value the 2-digit LLVAR prefix can express
    private const val AMOUNT_FIELD_LENGTH = 12
    private const val STAN_FIELD_LENGTH = 6

    private const val BITMAP_OFFSET = MTI_LENGTH
    private const val PAN_LENGTH_PREFIX_OFFSET = BITMAP_OFFSET + BITMAP_LENGTH
    private const val MIN_MESSAGE_LENGTH = PAN_LENGTH_PREFIX_OFFSET + PAN_LENGTH_PREFIX_LENGTH

    private fun requireAsciiDigits(value: String, field: String) {
        require(value.isNotEmpty() && value.all { it in '0'..'9' }) { "$field must contain only ASCII decimal digits, was '$value'" }
    }

    fun encode(request: TransactionRequest): ByteArray {
        requireAsciiDigits(request.pan, "PAN")
        requireAsciiDigits(request.stan, "STAN")
        require(request.pan.length <= PAN_MAX_LENGTH) { "PAN must fit the 2-digit LLVAR length prefix (DE2), was ${request.pan.length} digits" }
        require(request.stan.length <= STAN_FIELD_LENGTH) { "STAN must fit the fixed 6-digit field (DE11), was '${request.stan}'" }
        require(request.amount in 0..999_999_999_999) { "Amount must fit the 12-digit field (DE4), was ${request.amount}" }

        val mti = Mti.request(request.type)
        val bitmap = Bitmap.of(DE_2_PAN, DE_4_AMOUNT, DE_11_STAN)
        val panField = request.pan.length.toString().padStart(PAN_LENGTH_PREFIX_LENGTH, '0') + request.pan
        val amountField = request.amount.toString().padStart(AMOUNT_FIELD_LENGTH, '0')
        val stanField = request.stan.padStart(STAN_FIELD_LENGTH, '0')

        return mti.toByteArray(US_ASCII) +
                bitmap.toBytes() +
                panField.toByteArray(US_ASCII) +
                amountField.toByteArray(US_ASCII) +
                stanField.toByteArray(US_ASCII)
    }

    fun decode(bytes: ByteArray): TransactionRequest {
        require(bytes.size >= MIN_MESSAGE_LENGTH) {
            "Request must be at least $MIN_MESSAGE_LENGTH bytes to contain MTI, bitmap, and PAN length prefix, was ${bytes.size}"
        }
        val mti = String(bytes, 0, MTI_LENGTH, US_ASCII)
        val type = Mti.transactionType(mti)
        require(mti == Mti.request(type)) { "Expected request MTI ${Mti.request(type)} for $type, but got $mti" }
        val bitmap = Bitmap.decode(bytes.copyOfRange(BITMAP_OFFSET, PAN_LENGTH_PREFIX_OFFSET))
        require(bitmap == Bitmap.of(DE_2_PAN, DE_4_AMOUNT, DE_11_STAN)) { "Unexpected fields found in bitmap $bitmap" }

        val panLength = String(bytes, PAN_LENGTH_PREFIX_OFFSET, PAN_LENGTH_PREFIX_LENGTH, US_ASCII).toInt()
        val panOffset = PAN_LENGTH_PREFIX_OFFSET + PAN_LENGTH_PREFIX_LENGTH
        val amountOffset = panOffset + panLength
        val stanOffset = amountOffset + AMOUNT_FIELD_LENGTH
        val expectedLength = stanOffset + STAN_FIELD_LENGTH
        require(bytes.size == expectedLength) {
            "Request must be exactly $expectedLength bytes for PAN length $panLength, was ${bytes.size}"
        }

        val pan = String(bytes, panOffset, panLength, US_ASCII)
        requireAsciiDigits(pan, "PAN")

        val amountField = String(bytes, amountOffset, AMOUNT_FIELD_LENGTH, US_ASCII)
        requireAsciiDigits(amountField, "Amount")
        val amount = amountField.toLong()

        val stan = String(bytes, stanOffset, STAN_FIELD_LENGTH, US_ASCII)
        requireAsciiDigits(stan, "STAN")

        return TransactionRequest(type = type, stan = stan, pan = pan, amount = amount)
    }
}
