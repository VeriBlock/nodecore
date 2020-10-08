// VeriBlock Blockchain Project
// Copyright 2017-2018 VeriBlock, Inc
// Copyright 2018-2019 Xenios SEZC
// All rights reserved.
// https://www.veriblock.org
// Distributed under the MIT software license, see the accompanying
// file LICENSE or http://www.opensource.org/licenses/mit-license.php.

package org.veriblock.sdk.sqlite.tables;

import org.veriblock.core.crypto.Sha256Hash;
import org.veriblock.core.crypto.VBlakeHash;
import org.veriblock.core.utilities.Utility;
import org.veriblock.sdk.blockchain.store.StoredVeriBlockBlock;
import org.veriblock.sdk.models.VeriBlockBlock;
import org.veriblock.sdk.services.SerializeDeserializeService;

import java.math.BigInteger;
import java.nio.ByteBuffer;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Arrays;
import java.util.List;

public class VeriBlockBlockRepository extends GenericBlockRepository<StoredVeriBlockBlock, VBlakeHash> {

    public static final String TABLE_NAME = "veriBlockBlocks";

    private final static BlockSQLSerializer<StoredVeriBlockBlock, VBlakeHash> serializer =
        new BlockSQLSerializer<StoredVeriBlockBlock, VBlakeHash>() {

            public void toStmt(StoredVeriBlockBlock block, PreparedStatement stmt) throws SQLException {
                int i = 0;
                stmt.setObject(++i, new StringBuilder(Utility.bytesToHex(block.getHash().getBytes())).reverse().toString());
                stmt.setObject(++i, new StringBuilder(Utility.bytesToHex(block.getBlock().getPreviousBlock().getBytes())).reverse().toString());
                stmt.setObject(++i, block.getHeight());
                stmt.setObject(++i, block.getWork().toString());
                stmt.setObject(++i, Utility.bytesToHex(block.getBlockOfProof().getBytes()));
                stmt.setObject(++i, Utility.bytesToHex(SerializeDeserializeService.INSTANCE.serialize(block.getBlock())));
            }

            public StoredVeriBlockBlock fromResult(ResultSet result) throws SQLException {
                String hash = new StringBuilder(result.getString("id")).reverse().toString();
                VBlakeHash vBlakeHash = VBlakeHash.wrap(hash);
                if (vBlakeHash.length != 24) {
                    vBlakeHash = null;
                }
                byte[] data = Utility.hexToBytes(result.getString("data"));
                BigInteger work = new BigInteger(result.getString("work"));
                Sha256Hash blockOfProof = Sha256Hash.wrap(Utility.hexToBytes(result.getString("blockOfProof")));

                VeriBlockBlock block = SerializeDeserializeService.INSTANCE.parseVeriBlockBlock(ByteBuffer.wrap(data), vBlakeHash);
                StoredVeriBlockBlock storedBlock = new StoredVeriBlockBlock(block, work, block.getHash(), blockOfProof);
                return storedBlock;
            }

            public String getSchema() {
                return " db_id INTEGER PRIMARY KEY AUTOINCREMENT,"
                    + " id TEXT,"
                    + " previousId TEXT,"
                    + " height INTEGER,"
                    + " work TEXT,"
                    + " blockOfProof TEXT,"
                    + " data TEXT";
            }

            @Override
            public String addIndexes() {
                return "CREATE UNIQUE INDEX IF NOT EXISTS  vb_block_id_index ON " + TABLE_NAME + " (id);";
            }

            @Override
            public String removeIndexes() {
                return "DROP INDEX vb_block_id_index;";
            }

            public List<String> getColumns() {
                return Arrays.asList("id", "previousId", "height", "work", "blockOfProof", "data");
            }

            public String idToString(VBlakeHash hash) {
                return new StringBuilder(Utility.bytesToHex(hash.getBytes())).reverse().toString();
            }
        };

    public VeriBlockBlockRepository(Connection connection) throws SQLException {
        super(connection, TABLE_NAME, serializer);
    }
}
