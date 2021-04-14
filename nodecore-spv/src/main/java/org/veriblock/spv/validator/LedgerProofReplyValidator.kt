package org.veriblock.spv.validator

import nodecore.api.grpc.RpcLedgerProofReply
import org.slf4j.LoggerFactory
import org.veriblock.core.utilities.createLogger
import org.veriblock.extensions.ledger.LedgerProofWithContext
import org.veriblock.spv.net.SpvPeerTable

private val logger = createLogger {}

object LedgerProofReplyValidator {
    @JvmStatic
    fun validate(ledgerProofResult: RpcLedgerProofReply.LedgerProofResult): Boolean {
        try {
            LedgerProofWithContext.parseFrom(
                ledgerProofResult.ledgerProofWithContext
            )
        } catch (ex: Exception) {
            logger.warn(ex) {"LedgerProofWithContext is not valid. ${ex.message}" }
            //TODO SPV-67 add ban for peer.
            return false
        }
        return true
    }
}
