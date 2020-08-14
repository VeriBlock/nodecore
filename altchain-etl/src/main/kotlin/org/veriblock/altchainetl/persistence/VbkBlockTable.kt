package org.veriblock.altchainetl.persistence

import org.jetbrains.exposed.dao.id.LongIdTable

object VbkBlockTable : LongIdTable("vbk_block") {
    val hash = varchar("hash", 64)
    val prevHash = varchar("prev_hash", 64)
    val height = integer("height")
    val header = text("header")
}
