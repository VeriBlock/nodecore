package org.veriblock.altchainetl.persistence

import org.jetbrains.exposed.sql.Table

object AltBlockTable : Table("alt_block") {
    val hash = varchar("hash", 32)
    val prevHash = varchar("prev_hash", 32)
    val height = integer("height")
    val header = text("header")
    val atvs = text("atvs") // Should be of Array type
    val vtbs = text("vtbs") // Should be of Array type
    val vbkblocks = text("vbk_blocks") // Should be of Array type
    val transactions = text("transactions") // Should be of Array type

    override val primaryKey = PrimaryKey(hash)
}
