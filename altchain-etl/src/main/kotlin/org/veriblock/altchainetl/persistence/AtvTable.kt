package org.veriblock.altchainetl.persistence

import org.jetbrains.exposed.dao.id.LongIdTable

object AtvTable : LongIdTable("atv") {
    val vbkBoPHash = varchar("vbk_bop_hash", 64)
    val altContainingHash = varchar("alt_containing_hash", 64)
    val altContainingHeight = integer("alt_containing_height")
    val altEndorsedHeader = text("alt_endorsed_header")
    val payoutInfo = long("payout_info")
}
