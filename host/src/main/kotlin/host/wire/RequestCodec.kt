package host.wire

import host.domain.TransactionRequest
import java.nio.charset.StandardCharsets.US_ASCII

/** Translates between wire bytes and [TransactionRequest]: MTI + DE2 (PAN, LLVAR) + DE4 (amount) + DE11 (STAN). */
object RequestCodec {

    fun encode(request: TransactionRequest): ByteArray {
        require(request.pan.length <= 99) { "PAN must fit the 2-digit LLVAR length prefix (DE2), was ${request.pan.length} digits" }
        require(request.stan.length <= 6) { "STAN must fit the fixed 6-digit field (DE11), was '${request.stan}'" }

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
        val bitmap = Bitmap.decode(bytes.copyOfRange(4, 12))
        var offset = 12

        require(bitmap.contains(2)) { "DE2 (PAN) missing from request bitmap" }
        val panLength = String(bytes, offset, 2, US_ASCII).toInt()
        offset += 2
        val pan = String(bytes, offset, panLength, US_ASCII)
        offset += panLength

        require(bitmap.contains(4)) { "DE4 (amount) missing from request bitmap" }
        val amount = String(bytes, offset, 12, US_ASCII).toLong()
        offset += 12

        require(bitmap.contains(11)) { "DE11 (STAN) missing from request bitmap" }
        val stan = String(bytes, offset, 6, US_ASCII)

        return TransactionRequest(type = type, stan = stan, pan = pan, amount = amount)
    }
}
