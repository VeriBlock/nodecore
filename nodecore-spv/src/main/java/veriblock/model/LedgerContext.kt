package veriblock.model

import org.veriblock.sdk.models.Address

class LedgerContext : Comparable<LedgerContext> {
    var address: Address? = null
    var ledgerValue: LedgerValue? = null
    var ledgerProofStatus: LedgerProofStatus? = null
    var blockHeader: BlockHeader? = null

    override fun compareTo(other: LedgerContext): Int {
        if (ledgerProofStatus == null) {
            return 1
        }
        if (ledgerProofStatus == LedgerProofStatus.ADDRESS_DOES_NOT_EXIST
            && other.ledgerProofStatus == LedgerProofStatus.ADDRESS_EXISTS
        ) {
            return 1
        }
        if (other.ledgerProofStatus != LedgerProofStatus.ADDRESS_EXISTS) {
            return -1
        }
        if (ledgerValue!!.signatureIndex < other.ledgerValue!!.signatureIndex) {
            return 1
        } else if (ledgerValue!!.signatureIndex == other.ledgerValue!!.signatureIndex
            && ledgerValue!!.availableAtomicUnits < other.ledgerValue!!.availableAtomicUnits
        ) {
            return 1
        }
        return -1
    }
}
