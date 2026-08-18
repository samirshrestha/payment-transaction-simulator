package host.wire

import host.domain.TransactionResponse
import java.nio.charset.StandardCharsets.US_ASCII

/** Translates between wire bytes and [TransactionResponse]: MTI + DE11 (STAN) + DE39 (response code). */
object ResponseCodec {
    private const val APPROVED_CODE = "00"
    private const val DECLINED_CODE = "05"

    private val ALLOWED_RESPONSE_CODES = listOf(APPROVED_CODE, DECLINED_CODE)
    private val stanPatternRegEx = Regex("[0-9]{1,6}")   // input side: 1-6 digits, before padding
    private val stanFieldRegEx = Regex("[0-9]{6}")        // wire side: exactly 6 digits

    fun encode(response: TransactionResponse): ByteArray {
        require(stanPatternRegEx.matches(response.stan)) { "STAN must fit the fixed 6-digit only field (DE11), was '${response.stan}'" }

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
        require(bytes.size == 20) { "Response must be exactly 20 bytes, was ${bytes.size}" }

        val mti = String(bytes, 0, 4, US_ASCII)
        val type = Mti.transactionType(mti)
        require(mti == Mti.response(type)) { "Expected response MTI ${Mti.response(type)} for $type, but got $mti" }
        val bitmap = Bitmap.decode(bytes.copyOfRange(4, 12))
        require(bitmap == Bitmap.of(11, 39)) { "Unexpected fields found in bitmap $bitmap" }
        var offset = 12

        val stan = String(bytes, offset, 6, US_ASCII)
        require(stanFieldRegEx.matches(stan)) { "STAN must be 6 digit value, was $stan" }
        offset += 6

        val responseCode = String(bytes, offset, 2, US_ASCII)
        require(responseCode in ALLOWED_RESPONSE_CODES) { "Unrecognized response code set, $responseCode" }

        return TransactionResponse(
            type = type,
            stan = stan,
            approved = responseCode == APPROVED_CODE
        )
    }
}
