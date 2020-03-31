// VeriBlock PoP Miner
// Copyright 2017-2020 Xenios SEZC
// All rights reserved.
// https://www.veriblock.org
// Distributed under the MIT software license, see the accompanying
// file LICENSE or http://www.opensource.org/licenses/mit-license.php.
package org.veriblock.miners.pop.storage

import com.j256.ormlite.field.DataType
import com.j256.ormlite.field.DatabaseField
import com.j256.ormlite.table.DatabaseTable

@DatabaseTable(tableName = "operation_state")
class OperationStateData {
    @DatabaseField(columnName = "id", id = true)
    var id: String? = null

    @DatabaseField(columnName = "endorsed_block_number", index = true)
    var endorsedBlockNumber = 0

    @DatabaseField(columnName = "endorsed_block_hash", index = true)
    var endorsedBlockHash: String? = null

    @DatabaseField(columnName = "status")
    var status: String? = null

    @DatabaseField(columnName = "action")
    var action: String? = null

    @DatabaseField(columnName = "transaction_status", index = true)
    var transactionStatus: String? = null

    @DatabaseField(columnName = "message")
    var message: String? = null

    @DatabaseField(columnName = "state", dataType = DataType.BYTE_ARRAY)
    var state: ByteArray? = null

    @DatabaseField(columnName = "is_done")
    var isDone = false

    @DatabaseField(columnName = "last_updated", index = true)
    var lastUpdated = 0
}
