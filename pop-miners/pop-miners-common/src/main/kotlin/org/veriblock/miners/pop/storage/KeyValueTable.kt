package org.veriblock.miners.pop.storage

import org.jetbrains.exposed.sql.ResultRow
import org.jetbrains.exposed.sql.Table

object KeyValueTable : Table("app_values") {
    val key = varchar("key", 50)
    val value = varchar("value", 50)

    override val primaryKey = PrimaryKey(key)
}

data class KeyValueRecord(
    val key: String,
    val value: String
)

fun ResultRow.toKeyValueRecord() = KeyValueRecord(
    this[KeyValueTable.key],
    this[KeyValueTable.value]
)
