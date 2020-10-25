// VeriBlock Blockchain Project
// Copyright 2017-2018 VeriBlock, Inc
// Copyright 2018-2019 Xenios SEZC
// All rights reserved.
// https://www.veriblock.org
// Distributed under the MIT software license, see the accompanying
// file LICENSE or http://www.opensource.org/licenses/mit-license.php.
package org.veriblock.sdk.sqlite.tables

import org.veriblock.core.crypto.Sha256Hash
import org.veriblock.core.utilities.Utility
import org.veriblock.sdk.models.AltPublication
import org.veriblock.sdk.services.SerializeDeserializeService.serialize
import java.sql.Connection
import java.sql.SQLException
import java.sql.Statement

class AltPublicationRepository(private val connectionSource: Connection) {
    @Throws(SQLException::class)
    fun clear() {
        connectionSource.createStatement().use { stmt -> stmt.execute("DELETE FROM " + tableName) }
    }

    @Throws(SQLException::class)
    fun save(publication: AltPublication?): String {
        var hash: String
        val sql = " REPLACE INTO " + tableName + " ('" + altPublicationHash + "' , '" + altPublicationDataColumnName + "') " +
            "VALUES(?, ?) "
        connectionSource.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS).use { stmt ->
            val bytes = serialize(publication!!)
            hash = Utility.bytesToHex(Sha256Hash.hash(bytes))
            stmt.setString(1, hash)
            stmt.setBytes(2, bytes)
            stmt.executeUpdate()
        }
        return hash
    }

    companion object {
        const val tableName = "alt_publication"
        const val altPublicationHash = "hash"
        const val altPublicationDataColumnName = "data"
    }

    init {
        connectionSource.createStatement().use { stmt ->
            stmt.execute(
                """CREATE TABLE IF NOT EXISTS $tableName(
 $altPublicationHash TEXT PRIMARY KEY,
 $altPublicationDataColumnName BLOB NOT NULL
 );"""
            )
        }
        connectionSource.createStatement().use { stmt -> stmt.execute("PRAGMA journal_mode=WAL;") }
    }
}
