package org.veriblock.miners.pop.storage

import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.select
import org.jetbrains.exposed.sql.transactions.transaction

class KeyValueRepository(
    private val database: Database
) {
    fun insert(data: KeyValueRecord) = transaction(database) {
        KeyValueTable.insertOrUpdate(KeyValueTable.key) {
            it[key] = data.key
            it[value] = data.value
        }
    }

    operator fun get(key: String): KeyValueRecord? = transaction(database) {
        KeyValueTable.select {
            KeyValueTable.key eq key
        }.firstOrNull()?.toKeyValueRecord()
    }
}
