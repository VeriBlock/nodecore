package org.veriblock.spv.model

import org.veriblock.sdk.models.Address
import org.veriblock.sdk.models.VeriBlockBlock

class LedgerContext(
    val address: Address,
    val ledgerValue: LedgerValue,
    val block: VeriBlockBlock
) {
    override fun toString(): String {
        return "Address=$address $ledgerValue at $block"
    }
}
