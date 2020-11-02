package org.veriblock.spv.model.mapper

import nodecore.api.grpc.VeriBlockMessages
import nodecore.api.grpc.VeriBlockMessages.LedgerProofReply.LedgerProofResult
import org.veriblock.core.InvalidAddressException
import org.veriblock.core.bitcoinj.Base58
import org.veriblock.core.crypto.asVbkHash
import org.veriblock.sdk.models.Address
import org.veriblock.sdk.services.SerializeDeserializeService
import org.veriblock.spv.exception.SpvProcessException
import org.veriblock.spv.model.LedgerContext
import org.veriblock.spv.model.LedgerProofStatus
import org.veriblock.spv.model.LedgerValue

object LedgerProofReplyMapper {
    fun map(ledgerProofResult: LedgerProofResult): LedgerContext? {
        val address = Address(Base58.encode(ledgerProofResult.address.toByteArray()))
        val status: LedgerProofStatus = LedgerProofStatus.getByOrdinal(ledgerProofResult.result.number)
        val blockHeaderVB = ledgerProofResult.ledgerProofWithContext.blockHeader

        val block = try {
            SerializeDeserializeService.parseVeriBlockBlock(
                blockHeaderVB.header.toByteArray(),
                blockHeaderVB.hash.toByteArray().asVbkHash()
            )
        } catch (e: Exception) {
            return null
        }
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
            block = block
        )
    }

    private fun VeriBlockMessages.LedgerValue.toModel(): LedgerValue {
        return LedgerValue(availableAtomicUnits, frozenAtomicUnits, signatureIndex)
    }
}
