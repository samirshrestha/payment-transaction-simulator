package host.wire

import host.domain.TransactionResponse
import java.nio.charset.StandardCharsets.US_ASCII

/** Translates between wire bytes and [TransactionResponse]: MTI + DE11 (STAN) + DE39 (response code). */
object ResponseCodec {
    private const val APPROVED_CODE = "00"
    private const val DECLINED_CODE = "05"

    private const val DE_11_STAN = 11
    private const val DE_39_RESPONSE_CODE = 39

    private const val MTI_LENGTH = 4
    private const val BITMAP_LENGTH = 8
    private const val STAN_FIELD_LENGTH = 6
    private const val RESPONSE_CODE_LENGTH = 2

    private const val BITMAP_OFFSET = MTI_LENGTH
    private const val STAN_OFFSET = BITMAP_OFFSET + BITMAP_LENGTH
    private const val RESPONSE_CODE_OFFSET = STAN_OFFSET + STAN_FIELD_LENGTH
    private const val MESSAGE_LENGTH = RESPONSE_CODE_OFFSET + RESPONSE_CODE_LENGTH

    private val ALLOWED_RESPONSE_CODES = listOf(APPROVED_CODE, DECLINED_CODE)
    private val stanPatternRegEx = Regex("[0-9]{1,$STAN_FIELD_LENGTH}")   // input side: 1-6 digits, before padding
    private val stanFieldRegEx = Regex("[0-9]{$STAN_FIELD_LENGTH}")        // wire side: exactly 6 digits

    fun encode(response: TransactionResponse): ByteArray {
        require(stanPatternRegEx.matches(response.stan)) { "STAN must fit the fixed 6-digit only field (DE11), was '${response.stan}'" }

        val mti = Mti.response(response.type)
        val bitmap = Bitmap.of(DE_11_STAN, DE_39_RESPONSE_CODE)
        val stanField = response.stan.padStart(STAN_FIELD_LENGTH, '0')
        val responseCodeField = if (response.approved) APPROVED_CODE else DECLINED_CODE

        return mti.toByteArray(US_ASCII) +
                bitmap.toBytes() +
                stanField.toByteArray(US_ASCII) +
                responseCodeField.toByteArray(US_ASCII)
    }

    fun decode(bytes: ByteArray): TransactionResponse {
        require(bytes.size == MESSAGE_LENGTH) { "Response must be exactly $MESSAGE_LENGTH bytes, was ${bytes.size}" }

        val mti = String(bytes, 0, MTI_LENGTH, US_ASCII)
        val type = Mti.transactionType(mti)
        require(mti == Mti.response(type)) { "Expected response MTI ${Mti.response(type)} for $type, but got $mti" }
        val bitmap = Bitmap.decode(bytes.copyOfRange(BITMAP_OFFSET, STAN_OFFSET))
        require(bitmap == Bitmap.of(DE_11_STAN, DE_39_RESPONSE_CODE)) { "Unexpected fields found in bitmap $bitmap" }

        val stan = String(bytes, STAN_OFFSET, STAN_FIELD_LENGTH, US_ASCII)
        require(stanFieldRegEx.matches(stan)) { "STAN must be 6 digit value, was $stan" }

        val responseCode = String(bytes, RESPONSE_CODE_OFFSET, RESPONSE_CODE_LENGTH, US_ASCII)
        require(responseCode in ALLOWED_RESPONSE_CODES) { "Unrecognized response code set, $responseCode" }

        return TransactionResponse(
            type = type,
            stan = stan,
            approved = responseCode == APPROVED_CODE
        )
    }
}
