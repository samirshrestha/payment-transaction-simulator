package host.wire

import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertFailsWith

class BitmapTest {

    @Test
    fun `refuses invalid 0 field in Bitmap`() {
        assertFailsWith<IllegalArgumentException> { Bitmap.of(0) }
    }

    @Test
    fun `refuses unsupported 65 field in Bitmap`() {
        assertFailsWith<IllegalArgumentException> { Bitmap.of(65) }
    }

    @Test
    fun `encodes field 1 and field 64 to the first and last bit of the 8-byte array`() {
        val expected = byteArrayOf(0x80.toByte(), 0, 0, 0, 0, 0, 0, 0x01) // 0x80 = 128, needs .toByte() since Byte is signed
        assertContentEquals(expected, Bitmap.of(1, 64).toBytes())
    }
}