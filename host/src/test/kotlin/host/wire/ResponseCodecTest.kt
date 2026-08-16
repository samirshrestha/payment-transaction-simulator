package host.wire

import host.domain.TransactionResponse
import host.domain.TransactionType
import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class ResponseCodecTest {

    @Test
    fun `decodes a well-formed Authorization response`() {
        val mti = "0110".toByteArray(Charsets.US_ASCII)
        val bitmap = byteArrayOf(0, 0x20, 0, 0, 2, 0, 0, 0) // DE11, DE39 present
        val stan = "000001".toByteArray(Charsets.US_ASCII)
        val responseCode = "00".toByteArray(Charsets.US_ASCII)
        val message = mti + bitmap + stan + responseCode

        val response = ResponseCodec.decode(message)

        assertEquals(
            TransactionResponse(type = TransactionType.AUTHORIZATION, stan = "000001", approved = true),
            response,
        )
    }

    @Test
    fun `encodes a declined Financial response onto the wire`() {
        val response = TransactionResponse(type = TransactionType.FINANCIAL, stan = "000042", approved = false)

        val encoded = ResponseCodec.encode(response)

        val expectedMti = "0210".toByteArray(Charsets.US_ASCII)
        val expectedBitmap = byteArrayOf(0, 0x20, 0, 0, 2, 0, 0, 0)
        val expectedStan = "000042".toByteArray(Charsets.US_ASCII)
        val expectedResponseCode = "05".toByteArray(Charsets.US_ASCII)
        val expected = expectedMti + expectedBitmap + expectedStan + expectedResponseCode

        assertContentEquals(expected, encoded)
    }

    @Test
    fun `refuses to encode a STAN that doesn't fit the fixed 6-digit field`() {
        val response = TransactionResponse(type = TransactionType.AUTHORIZATION, stan = "1234567", approved = true)

        assertFailsWith<IllegalArgumentException> { ResponseCodec.encode(response) }
    }
}
