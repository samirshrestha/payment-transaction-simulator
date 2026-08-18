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

    @Test
    fun `refuses to encode a non-numeric STAN`() {
        val response = TransactionResponse(type = TransactionType.AUTHORIZATION, stan = "12a456", approved = true)

        assertFailsWith<IllegalArgumentException> { ResponseCodec.encode(response) }
    }

    @Test
    fun `refuses to decode a non-numeric STAN`() {
        val mti = "0110".toByteArray(Charsets.US_ASCII)
        val bitmap = byteArrayOf(0, 0x20, 0, 0, 2, 0, 0, 0) // DE11, DE39 present
        val stan = "12a456".toByteArray(Charsets.US_ASCII)
        val responseCode = "00".toByteArray(Charsets.US_ASCII)
        val message = mti + bitmap + stan + responseCode

        assertFailsWith<IllegalArgumentException> { ResponseCodec.decode(message) }
    }

    @Test
    fun `refuses to decode unrecognized response code` () {
        val mti = "0110".toByteArray(Charsets.US_ASCII)
        val bitmap = byteArrayOf(0, 0x20, 0, 0, 2, 0, 0, 0) // DE11, DE39 present
        val stan = "000001".toByteArray(Charsets.US_ASCII)
        val responseCode = "XX".toByteArray(Charsets.US_ASCII)
        val message = mti + bitmap + stan + responseCode

        assertFailsWith<IllegalArgumentException> { ResponseCodec.decode(message) }
    }

    @Test
    fun `refuses to decode a response with a request MTI`() {
        val mti = "0100".toByteArray(Charsets.US_ASCII) // Authorization request, not response
        val bitmap = byteArrayOf(0, 0x20, 0, 0, 2, 0, 0, 0) // DE11, DE39 present
        val stan = "000001".toByteArray(Charsets.US_ASCII)
        val responseCode = "00".toByteArray(Charsets.US_ASCII)
        val message = mti + bitmap + stan + responseCode

        assertFailsWith<IllegalArgumentException> { ResponseCodec.decode(message) }
    }

    @Test
    fun `refuses to decode a response with unexpected field(s)`() {
        val mti = "0110".toByteArray(Charsets.US_ASCII)
        val bitmap = byteArrayOf(0, 0x20, 0, 0, 0x06, 0, 0, 0) // DE11, DE38, DE39 present
        val stan = "000001".toByteArray(Charsets.US_ASCII)
        // DE38, not a field this codec knows. Starts with "00" deliberately: if the bitmap-exactness
        // check were missing, decode() would misread these bytes' first 2 chars as DE39 and they'd
        // happen to equal APPROVED_CODE, masking the missing check behind the response-code guard.
        val approvalCode = "00A1B2".toByteArray(Charsets.US_ASCII)
        val responseCode = "00".toByteArray(Charsets.US_ASCII)
        val message = mti + bitmap + stan + approvalCode + responseCode

        assertFailsWith<IllegalArgumentException> { ResponseCodec.decode(message) }
    }

    @Test
    fun `refuses to decode a response with trailing bytes`() {
        val mti = "0110".toByteArray(Charsets.US_ASCII)
        val bitmap = byteArrayOf(0, 0x20, 0, 0, 2, 0, 0, 0) // DE11, DE39 present
        val stan = "000001".toByteArray(Charsets.US_ASCII)
        val responseCode = "00".toByteArray(Charsets.US_ASCII)
        val trailingGarbage = byteArrayOf(0x7A, 0x7A) // 2 extra bytes nobody declared
        val message = mti + bitmap + stan + responseCode + trailingGarbage

        assertFailsWith<IllegalArgumentException> { ResponseCodec.decode(message) }
    }

    @Test
    fun `refuses to decode a truncated response`() {
        val mti = "0110".toByteArray(Charsets.US_ASCII)
        val bitmap = byteArrayOf(0, 0x20, 0, 0, 2, 0, 0, 0) // DE11, DE39 present
        val stan = "000001".toByteArray(Charsets.US_ASCII)
        val message = mti + bitmap + stan // missing the 2-byte response code entirely

        assertFailsWith<IllegalArgumentException> { ResponseCodec.decode(message) }
    }
}
