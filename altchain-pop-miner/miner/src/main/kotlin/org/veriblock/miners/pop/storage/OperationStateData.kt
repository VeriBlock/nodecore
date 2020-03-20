// VeriBlock Blockchain Project
// Copyright 2017-2018 VeriBlock, Inc
// Copyright 2018-2020 Xenios SEZC
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
    @DatabaseField(columnName = "status")
    var status: Int = 0
    @DatabaseField(columnName = "state", dataType = DataType.BYTE_ARRAY)
    var state: ByteArray? = null
}
