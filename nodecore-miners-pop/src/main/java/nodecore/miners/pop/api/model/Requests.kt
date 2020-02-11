// VeriBlock NodeCore
// Copyright 2017-2019 Xenios SEZC
// All rights reserved.
// https://www.veriblock.org
// Distributed under the MIT software license, see the accompanying
// file LICENSE or http://www.opensource.org/licenses/mit-license.php.

package nodecore.miners.pop.api.model

class EmptyRequest

class MineRequest(
    val block: Int?
)

class WithdrawRequest(
    var destinationAddress: String?,
    var amount: String?
)

class SetConfigRequest(
    val key: String?,
    val value: String?
)
