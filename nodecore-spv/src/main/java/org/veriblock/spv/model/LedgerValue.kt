package org.veriblock.spv.model

class LedgerValue(
    val availableAtomicUnits: Long,
    val frozenAtomicUnits: Long,
    val signatureIndex: Long
) {
    override fun toString(): String {
        return "available=$availableAtomicUnits frozen=$frozenAtomicUnits sigIndex=$signatureIndex"
    }
}
