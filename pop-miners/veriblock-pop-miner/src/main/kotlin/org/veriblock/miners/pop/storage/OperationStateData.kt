// VeriBlock PoP Miner
// Copyright 2017-2020 Xenios SEZC
// All rights reserved.
// https://www.veriblock.org
// Distributed under the MIT software license, see the accompanying
// file LICENSE or http://www.opensource.org/licenses/mit-license.php.

package org.veriblock.miners.pop.storage;

import com.j256.ormlite.field.DataType;
import com.j256.ormlite.field.DatabaseField;
import com.j256.ormlite.table.DatabaseTable;

@DatabaseTable(tableName = "operation_state")
public class OperationStateData {
    @DatabaseField(columnName = "id", id = true)
    public String id;
    @DatabaseField(columnName = "endorsed_block_number", index = true)
    public int endorsedBlockNumber;
    @DatabaseField(columnName = "endorsed_block_hash", index = true)
    public String endorsedBlockHash;
    @DatabaseField(columnName = "status")
    public String status;
    @DatabaseField(columnName = "action")
    public String action;
    @DatabaseField(columnName = "transaction_status", index = true)
    public String transactionStatus;
    @DatabaseField(columnName = "message")
    public String message;
    @DatabaseField(columnName = "state", dataType = DataType.BYTE_ARRAY)
    public byte[] state;
    @DatabaseField(columnName = "is_done")
    public boolean isDone;
    @DatabaseField(columnName = "last_updated", index = true)
    public int lastUpdated;
}
