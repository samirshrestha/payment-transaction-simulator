package host.wire

import host.domain.TransactionRequest
import host.domain.TransactionType
import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class RequestCodecTest {

    @Test
    fun `decodes a well-formed Authorization request`() {
        val mti = "0100".toByteArray(Charsets.US_ASCII)
        val bitmap = byteArrayOf(0x50, 0x20, 0, 0, 0, 0, 0, 0) // DE2, DE4, DE11 present
        val pan = "164111111111111111".toByteArray(Charsets.US_ASCII) // LLVAR: 16-digit PAN
        val amount = "000000012345".toByteArray(Charsets.US_ASCII) // $123.45 in minor units
        val stan = "000001".toByteArray(Charsets.US_ASCII)
        val message = mti + bitmap + pan + amount + stan

        val request = RequestCodec.decode(message)

        assertEquals(
            TransactionRequest(
                type = TransactionType.AUTHORIZATION,
                stan = "000001",
                pan = "4111111111111111",
                amount = 12345L,
            ),
            request,
        )
    }

    @Test
    fun `encodes a Financial request onto the wire`() {
        val request = TransactionRequest(
            type = TransactionType.FINANCIAL,
            stan = "000042",
            pan = "5555555555554444",
            amount = 500L,
        )

        val encoded = RequestCodec.encode(request)

        val expectedMti = "0200".toByteArray(Charsets.US_ASCII)
        val expectedBitmap = byteArrayOf(0x50, 0x20, 0, 0, 0, 0, 0, 0)
        val expectedPan = "165555555555554444".toByteArray(Charsets.US_ASCII)
        val expectedAmount = "000000000500".toByteArray(Charsets.US_ASCII)
        val expectedStan = "000042".toByteArray(Charsets.US_ASCII)
        val expected = expectedMti + expectedBitmap + expectedPan + expectedAmount + expectedStan

        assertContentEquals(expected, encoded)
    }

    @Test
    fun `refuses to encode a STAN that doesn't fit the fixed 6-digit field`() {
        val request = TransactionRequest(
            type = TransactionType.AUTHORIZATION,
            stan = "1234567",
            pan = "4111111111111111",
            amount = 100L,
        )

        assertFailsWith<IllegalArgumentException> { RequestCodec.encode(request) }
    }

    @Test
    fun `refuses to encode Amount that exceeds 12-digit`() {
        val request = TransactionRequest(
            type = TransactionType.AUTHORIZATION,
            stan = "123456",
            pan = "4111111111111111",
            amount = 1_000_000_000_000L
        )

        assertFailsWith<IllegalArgumentException> { RequestCodec.encode(request) }
    }

    @Test
    fun `encodes Amount 999_999_999_999`() {
        val request = TransactionRequest(
            type = TransactionType.FINANCIAL,
            stan = "000042",
            pan = "5555555555554444",
            amount = 999_999_999_999,
        )

        val encoded = RequestCodec.encode(request)

        val expectedMti = "0200".toByteArray(Charsets.US_ASCII)
        val expectedBitmap = byteArrayOf(0x50, 0x20, 0, 0, 0, 0, 0, 0)
        val expectedPan = "165555555555554444".toByteArray(Charsets.US_ASCII)
        val expectedAmount = "999999999999".toByteArray(Charsets.US_ASCII)
        val expectedStan = "000042".toByteArray(Charsets.US_ASCII)
        val expected = expectedMti + expectedBitmap + expectedPan + expectedAmount + expectedStan

        assertContentEquals(expected, encoded)
    }

    @Test
    fun `refuses to decode Authorization request with bad-MTI`() {
        val mti = "0110".toByteArray(Charsets.US_ASCII)
        val bitmap = byteArrayOf(0x50, 0x20, 0, 0, 0, 0, 0, 0) // DE2, DE4, DE11 present
        val pan = "164111111111111111".toByteArray(Charsets.US_ASCII) // LLVAR: 16-digit PAN
        val amount = "000000012345".toByteArray(Charsets.US_ASCII) // $123.45 in minor units
        val stan = "000001".toByteArray(Charsets.US_ASCII)
        val message = mti + bitmap + pan + amount + stan

        assertFailsWith<IllegalArgumentException> { RequestCodec.decode(message) }
    }

    @Test
    fun `refuses to decode Financial request with unexpected field(s)`() {
        val mti = "0200".toByteArray(Charsets.US_ASCII)
        val bitmap = byteArrayOf(0x52, 0x20, 0, 0, 0, 0, 0, 0) // DE2, DE4, DE11 present
        val pan = "164111111111111111".toByteArray(Charsets.US_ASCII) // LLVAR: 16-digit PAN
        val transmissionDateTime = "0817081530".toByteArray(Charsets.US_ASCII) // 08/17 08:15:30
        val amount = "000000012345".toByteArray(Charsets.US_ASCII) // $123.45 in minor units
        val stan = "000001".toByteArray(Charsets.US_ASCII)
        val message = mti + bitmap + pan + transmissionDateTime + amount + stan

        assertFailsWith<IllegalArgumentException> { RequestCodec.decode(message) }
    }
}
