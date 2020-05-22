package veriblock.model.mapper

import nodecore.api.grpc.VeriBlockMessages
import nodecore.api.grpc.VeriBlockMessages.LedgerProofReply.LedgerProofResult
import org.veriblock.core.bitcoinj.Base58
import org.veriblock.sdk.models.Address
import veriblock.exception.SpvProcessException
import veriblock.model.BlockHeader
import veriblock.model.LedgerContext
import veriblock.model.LedgerProofStatus
import veriblock.model.LedgerValue
import java.util.ArrayList

object LedgerProofReplyMapper {
    fun map(ledgerProofResults: List<LedgerProofResult>): List<LedgerContext> {
        val ledgerContexts: MutableList<LedgerContext> = ArrayList()
        for (ledgerProofResult in ledgerProofResults) {
            ledgerContexts.add(map(ledgerProofResult))
        }
        return ledgerContexts
    }

    fun map(ledgerProofResult: LedgerProofResult): LedgerContext {
        val ledgerContext = LedgerContext()
        val address = Address(Base58.encode(ledgerProofResult.address.toByteArray()))
        val status: LedgerProofStatus = LedgerProofStatus.getByOrdinal(
            ledgerProofResult.result.number
        )
        val blockHeaderVB = ledgerProofResult.ledgerProofWithContext.blockHeader
        val blockHeader = BlockHeader(
            blockHeaderVB.header.toByteArray(), blockHeaderVB.hash.toByteArray()
        )
        if (status.exists()) {
            val ledgerValues: List<LedgerValue> = ledgerProofResult.ledgerProofWithContext.ledgerProof.proofOfExistence.verticalProofLayersList.map {
                map(it.ledgerValue)
            }
            if (ledgerValues.isEmpty()) {
                throw SpvProcessException("Ledger proof reply doesn't have ledger value.")
            }
            ledgerContext.ledgerValue = ledgerValues[0]
        }
        ledgerContext.address = address
        ledgerContext.ledgerProofStatus = status
        ledgerContext.blockHeader = blockHeader
        return ledgerContext
    }

    private fun map(ledgerProofReply: VeriBlockMessages.LedgerValue): LedgerValue {
        return LedgerValue(
            ledgerProofReply.availableAtomicUnits, ledgerProofReply.frozenAtomicUnits,
            ledgerProofReply.signatureIndex
        )
    }
}
