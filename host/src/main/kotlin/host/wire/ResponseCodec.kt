package host.wire

import host.domain.TransactionResponse
import java.nio.charset.StandardCharsets.US_ASCII

/** Translates between wire bytes and [TransactionResponse]: MTI + DE11 (STAN) + DE39 (response code). */
object ResponseCodec {
    private const val APPROVED_CODE = "00"
    private const val DECLINED_CODE = "05"

    fun encode(response: TransactionResponse): ByteArray {
        require(response.stan.length <= 6) { "STAN must fit the fixed 6-digit field (DE11), was '${response.stan}'" }

        val mti = Mti.response(response.type)
        val bitmap = Bitmap.of(11, 39)
        val stanField = response.stan.padStart(6, '0')
        val responseCodeField = if (response.approved) APPROVED_CODE else DECLINED_CODE

        return mti.toByteArray(US_ASCII) +
            bitmap.toBytes() +
            stanField.toByteArray(US_ASCII) +
            responseCodeField.toByteArray(US_ASCII)
    }

    fun decode(bytes: ByteArray): TransactionResponse {
        val mti = String(bytes, 0, 4, US_ASCII)
        val type = Mti.transactionType(mti)
        val bitmap = Bitmap.decode(bytes.copyOfRange(4, 12))
        var offset = 12

        require(bitmap.contains(11)) { "DE11 (STAN) missing from response bitmap" }
        val stan = String(bytes, offset, 6, US_ASCII)
        offset += 6

        require(bitmap.contains(39)) { "DE39 (response code) missing from response bitmap" }
        val responseCode = String(bytes, offset, 2, US_ASCII)

        return TransactionResponse(type = type, stan = stan, approved = responseCode == APPROVED_CODE)
    }
}
