package org.veriblock.altchainetl.persistence

import org.jetbrains.exposed.dao.id.LongIdTable

object TxTable : LongIdTable("tx") {
    val inputs = text("inputs") // Should be of Array type
    val outputs = text("outputs") // Should be of Array type
}
