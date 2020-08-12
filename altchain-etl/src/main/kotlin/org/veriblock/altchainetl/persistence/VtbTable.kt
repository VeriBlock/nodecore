package org.veriblock.altchainetl.persistence

import org.jetbrains.exposed.dao.id.LongIdTable

object VtbTable : LongIdTable("vtb") {
    val vbkContainingHash = varchar("vbk_containing_hash", 64)
    val altContainingHash = varchar("alt_containing_hash", 64)
    val vbkEndorsedHash = varchar("vbk_endorsed_hash", 64)
    val btcBoPHash = varchar("btc_bop_hash", 64)
}
