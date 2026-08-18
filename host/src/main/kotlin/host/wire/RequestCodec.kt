package host.wire

import host.domain.TransactionRequest
import java.nio.charset.StandardCharsets.US_ASCII

/** Translates between wire bytes and [TransactionRequest]: MTI + DE2 (PAN, LLVAR) + DE4 (amount) + DE11 (STAN). */
object RequestCodec {

    private fun requireAsciiDigits(value: String, field: String) {
        require(value.isNotEmpty() && value.all { it in '0'..'9' }) { "$field must contain only ASCII decimal digits, was '$value'" }
    }

    fun encode(request: TransactionRequest): ByteArray {
        requireAsciiDigits(request.pan, "PAN")
        requireAsciiDigits(request.stan, "STAN")
        require(request.pan.length <= 99) { "PAN must fit the 2-digit LLVAR length prefix (DE2), was ${request.pan.length} digits" }
        require(request.stan.length <= 6) { "STAN must fit the fixed 6-digit field (DE11), was '${request.stan}'" }
        require(request.amount in 0..999_999_999_999) { "Amount must fit the 12-digit field (DE4), was ${request.amount}" }

        val mti = Mti.request(request.type)
        val bitmap = Bitmap.of(2, 4, 11)
        val panField = request.pan.length.toString().padStart(2, '0') + request.pan
        val amountField = request.amount.toString().padStart(12, '0')
        val stanField = request.stan.padStart(6, '0')

        return mti.toByteArray(US_ASCII) +
                bitmap.toBytes() +
                panField.toByteArray(US_ASCII) +
                amountField.toByteArray(US_ASCII) +
                stanField.toByteArray(US_ASCII)
    }

    fun decode(bytes: ByteArray): TransactionRequest {
        val mti = String(bytes, 0, 4, US_ASCII)
        val type = Mti.transactionType(mti)
        require(mti == Mti.request(type)) { "Expected request MTI ${Mti.request(type)} for $type, but got $mti" }
        val bitmap = Bitmap.decode(bytes.copyOfRange(4, 12))
        require(bitmap == Bitmap.of(2, 4, 11)) { "Unexpected fields found in bitmap $bitmap" }
        var offset = 12

        val panLength = String(bytes, offset, 2, US_ASCII).toInt()
        offset += 2
        val pan = String(bytes, offset, panLength, US_ASCII)
        requireAsciiDigits(pan, "PAN")
        offset += panLength

        val amountField = String(bytes, offset, 12, US_ASCII)
        requireAsciiDigits(amountField, "Amount")
        val amount = amountField.toLong()
        offset += 12

        val stan = String(bytes, offset, 6, US_ASCII)
        requireAsciiDigits(stan, "STAN")

        return TransactionRequest(type = type, stan = stan, pan = pan, amount = amount)
    }
}
