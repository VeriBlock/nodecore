package veriblock.model

enum class LedgerProofStatus {
    ADDRESS_EXISTS,
    ADDRESS_DOES_NOT_EXIST,
    ADDRESS_IS_INVALID;

    fun exists(): Boolean =
        this == ADDRESS_EXISTS

    companion object {
        fun getByOrdinal(ordinal: Int): LedgerProofStatus {
            return values()[ordinal]
        }
    }
}
