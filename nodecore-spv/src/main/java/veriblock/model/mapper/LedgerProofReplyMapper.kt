package veriblock.model.mapper

import nodecore.api.grpc.VeriBlockMessages
import nodecore.api.grpc.VeriBlockMessages.LedgerProofReply.LedgerProofResult
import org.veriblock.core.InvalidAddressException
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
        val address = Address(Base58.encode(ledgerProofResult.address.toByteArray()))
        val status: LedgerProofStatus = LedgerProofStatus.getByOrdinal(ledgerProofResult.result.number)
        val blockHeaderVB = ledgerProofResult.ledgerProofWithContext.blockHeader
        val blockHeader = BlockHeader(
            blockHeaderVB.header.toByteArray(), blockHeaderVB.hash.toByteArray()
        )
        val ledgerValue: LedgerValue = when (status) {
            LedgerProofStatus.ADDRESS_EXISTS -> {
                val ledgerProofNode = ledgerProofResult.ledgerProofWithContext.ledgerProof.proofOfExistence.verticalProofLayersList.firstOrNull()
                    ?: throw SpvProcessException("Ledger proof reply doesn't have ledger value.")
                ledgerProofNode.ledgerValue.toModel()
            }
            LedgerProofStatus.ADDRESS_DOES_NOT_EXIST ->
                LedgerValue(0, 0, -1)
            else ->
                throw InvalidAddressException("Address (${address.address}) status is set to $status")
        }
        return LedgerContext(
            address = address,
            ledgerValue = ledgerValue,
            ledgerProofStatus = status,
            blockHeader = blockHeader
        )
    }

    private fun VeriBlockMessages.LedgerValue.toModel(): LedgerValue {
        return LedgerValue(availableAtomicUnits, frozenAtomicUnits, signatureIndex)
    }
}
