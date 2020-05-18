package org.veriblock.miners.pop.storage

import org.jetbrains.exposed.sql.Column
import org.jetbrains.exposed.sql.Table
import org.jetbrains.exposed.sql.Transaction
import org.jetbrains.exposed.sql.statements.InsertStatement
import org.jetbrains.exposed.sql.transactions.TransactionManager

/**
 * Statement using SQLite upsert.
 */
class InsertOrUpdate<Key : Any>(
    table: Table,
    isIgnore: Boolean = false,
    private vararg val keys: Column<*>
) : InsertStatement<Key>(table, isIgnore) {

    override fun prepareSQL(transaction: Transaction): String {
        val tm = TransactionManager.current()
        val updateSetter = table.columns.joinToString { "${tm.identity(it)} = EXCLUDED.${tm.identity(it)}" }
        val onConflict = "ON CONFLICT (${keys.joinToString { tm.identity(it) }}) DO UPDATE SET $updateSetter"
        return "${super.prepareSQL(transaction)} $onConflict"
    }
}

/**
 * Inserts or updates on conflict with [keys].
 *
 * Usage - pollId and userId is composite primary key, intention is to insert new pollOption or update current one.
```kotlin
Votes.insertOrUpdate(Votes.pollId, Votes.userId) {
it[pollId] = pollAction.pollId
it[pollOption] = pollAction.optionId
it[userId] = pollAction.userId
}
```
 */
fun <T : Table> T.insertOrUpdate(vararg keys: Column<*>, body: T.(InsertStatement<Number>) -> Unit) =
    InsertOrUpdate<Number>(this, keys = *keys).apply {
        body(this)
        execute(TransactionManager.current())
    }
