package org.veriblock.altchainetl.persistence

import org.jetbrains.exposed.sql.Table

object BtcBlockTable : Table("btc_block") {
    val hash = varchar("hash", 64)
    val prevHash = varchar("prev_hash", 64)
    val height = integer("height")
    val header = text("header")

    override val primaryKey = PrimaryKey(hash)
}
