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
import org.veriblock.sdk.blockchain.store.StoredBitcoinBlock
import org.veriblock.sdk.services.SerializeDeserializeService.parseBitcoinBlockWithLength
import org.veriblock.sdk.services.SerializeDeserializeService.serialize
import java.math.BigInteger
import java.nio.ByteBuffer
import java.sql.Connection
import java.sql.PreparedStatement
import java.sql.ResultSet
import java.sql.SQLException
import java.util.Arrays

class BitcoinBlockRepository(connection: Connection) : GenericBlockRepository<StoredBitcoinBlock, Sha256Hash>(connection, TABLE_NAME, serializer) {
    companion object {
        const val TABLE_NAME = "bitcoinBlocks"
        private val serializer: BlockSQLSerializer<StoredBitcoinBlock, Sha256Hash> = object : BlockSQLSerializer<StoredBitcoinBlock, Sha256Hash> {
            @Throws(SQLException::class)
            override fun toStmt(block: StoredBitcoinBlock, stmt: PreparedStatement) {
                var i = 0
                stmt.setObject(++i, Utility.bytesToHex(block.hash.bytes))
                stmt.setObject(++i, Utility.bytesToHex(block.block.previousBlock.bytes))
                stmt.setObject(++i, block.height)
                stmt.setObject(++i, block.work.toString())
                stmt.setObject(++i, Utility.bytesToHex(serialize(block.block)))
            }

            @Throws(SQLException::class)
            override fun fromResult(result: ResultSet): StoredBitcoinBlock {
                val data = Utility.hexToBytes(result.getString("data"))
                val work = BigInteger(result.getString("work"))
                val height = result.getInt("height")
                val block = parseBitcoinBlockWithLength(ByteBuffer.wrap(data))
                return StoredBitcoinBlock(block, work, height)
            }

            override fun getSchema(): String {
                return (" db_id INTEGER PRIMARY KEY AUTOINCREMENT,"
                    + " id TEXT,"
                    + " previousId TEXT,"
                    + " height INTEGER,"
                    + " work TEXT,"
                    + " data TEXT")
            }

            override fun addIndexes(): String {
                return "CREATE UNIQUE INDEX IF NOT EXISTS btc_block_id_index ON " + TABLE_NAME + " (id);"
            }

            override fun removeIndexes(): String {
                return "DROP INDEX btc_block_id_index;"
            }

            override fun getColumns(): List<String> {
                return Arrays.asList("id", "previousId", "height", "work", "data")
            }

            override fun idToString(hash: Sha256Hash): String {
                return Utility.bytesToHex(hash.bytes)
            }
        }
    }
}
