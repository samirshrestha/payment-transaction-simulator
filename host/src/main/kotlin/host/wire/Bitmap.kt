package host.wire

/** ISO 8583 primary bitmap: one bit per data element 1-64, MSB-first within each byte. */
@ConsistentCopyVisibility
internal data class Bitmap private constructor(private val fields: Set<Int>) {

    fun contains(field: Int): Boolean = field in fields

    fun toBytes(): ByteArray {
        val bytes = ByteArray(8)
        for (field in fields) {
            val (byteIndex, bitMask) = positionOf(field)
            bytes[byteIndex] = (bytes[byteIndex].toInt() or bitMask).toByte()
        }
        return bytes
    }

    companion object {
        fun of(vararg fields: Int): Bitmap {
            require(fields.all { it in 1..64}) { "Bitmap fields must be in 1..64, but was ${fields.contentToString()}"}
            return Bitmap(fields.toSet())
        }

        fun decode(bytes: ByteArray): Bitmap {
            require(bytes.size == 8) { "Primary bitmap must be exactly 8 bytes, was ${bytes.size}" }
            val fields = (1..64).filter { field ->
                val (byteIndex, bitMask) = positionOf(field)
                bytes[byteIndex].toInt() and bitMask != 0
            }
            return Bitmap(fields.toSet())
        }

        /** Byte index and bit mask for data element [field] within the 8-byte primary bitmap. */
        private fun positionOf(field: Int): Pair<Int, Int> =
            (field - 1) / 8 to (0x80 shr ((field - 1) % 8))
    }
}
