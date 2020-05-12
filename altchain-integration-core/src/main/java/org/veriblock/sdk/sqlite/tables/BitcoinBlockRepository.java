// VeriBlock Blockchain Project
// Copyright 2017-2018 VeriBlock, Inc
// Copyright 2018-2019 Xenios SEZC
// All rights reserved.
// https://www.veriblock.org
// Distributed under the MIT software license, see the accompanying
// file LICENSE or http://www.opensource.org/licenses/mit-license.php.

package org.veriblock.sdk.sqlite.tables;

import org.veriblock.sdk.blockchain.store.StoredBitcoinBlock;
import org.veriblock.sdk.models.BitcoinBlock;
import org.veriblock.sdk.models.Sha256Hash;
import org.veriblock.sdk.services.SerializeDeserializeService;
import org.veriblock.sdk.util.Utils;

import java.math.BigInteger;
import java.nio.ByteBuffer;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Arrays;
import java.util.List;

public class BitcoinBlockRepository extends GenericBlockRepository<StoredBitcoinBlock, Sha256Hash> {

    public static final String TABLE_NAME = "bitcoinBlocks";

    private final static BlockSQLSerializer<StoredBitcoinBlock, Sha256Hash> serializer =
                     new BlockSQLSerializer<StoredBitcoinBlock, Sha256Hash>() {

        public void toStmt(StoredBitcoinBlock block, PreparedStatement stmt) throws SQLException {
            int i = 0;
            stmt.setObject(++i, Utils.encodeHex(block.getHash().getBytes()));
            stmt.setObject(++i, Utils.encodeHex(block.getBlock().getPreviousBlock().getBytes()));
            stmt.setObject(++i, block.getHeight());
            stmt.setObject(++i, block.getWork().toString());
            stmt.setObject(++i, Utils.encodeHex(SerializeDeserializeService.serialize(block.getBlock())));
        }

        public StoredBitcoinBlock fromResult(ResultSet result) throws SQLException {
            byte[] data = Utils.decodeHex(result.getString("data"));
            BigInteger work = new BigInteger(result.getString("work"));
            int height = result.getInt("height");

            BitcoinBlock block = SerializeDeserializeService.parseBitcoinBlockWithLength(ByteBuffer.wrap(data));
            StoredBitcoinBlock storedBlock = new StoredBitcoinBlock(block, work, height);
            return storedBlock;
        }

        public String getSchema() {
            return  " db_id INTEGER PRIMARY KEY AUTOINCREMENT,"
                  + " id TEXT,"
                  + " previousId TEXT,"
                  + " height INTEGER,"
                  + " work TEXT,"
                  + " data TEXT";
        }

         @Override
         public String addIndexes() {
             return "CREATE UNIQUE INDEX IF NOT EXISTS btc_block_id_index ON " + TABLE_NAME + " (id);";
         }

         @Override
         public String removeIndexes() {
             return "DROP INDEX btc_block_id_index;";
         }

        public List<String> getColumns() {
            return Arrays.asList("id", "previousId", "height", "work", "data");
        }

        public String idToString(Sha256Hash hash) {
            return Utils.encodeHex(hash.getBytes());
        }
    };

    public BitcoinBlockRepository(Connection connection) throws SQLException {
        super(connection, TABLE_NAME, serializer);
    }
}
