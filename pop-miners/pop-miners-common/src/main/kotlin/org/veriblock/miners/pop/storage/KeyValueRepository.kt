package org.veriblock.miners.pop.storage

import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.select
import org.jetbrains.exposed.sql.transactions.transaction
import org.jetbrains.exposed.sql.update

class KeyValueRepository(
    private val database: Database
) {
    fun insert(data: KeyValueRecord) {
        if (get(data.key) != null) {
            transaction(database) {
                KeyValueTable.update({
                    KeyValueTable.key eq data.key
                }) {
                    it[value] = data.value
                }
            }
        } else {
            transaction(database) {
                KeyValueTable.insert {
                    it[key] = data.key
                    it[value] = data.value
                }
            }
        }
    }

    operator fun get(key: String): KeyValueRecord? = transaction(database) {
        KeyValueTable.select {
            KeyValueTable.key eq key
        }.firstOrNull()?.toKeyValueRecord()
    }
}
