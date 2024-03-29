// VeriBlock NodeCore
// Copyright 2017-2021 Xenios SEZC
// All rights reserved.
// https://www.veriblock.org
// Distributed under the MIT software license, see the accompanying
// file LICENSE or http://www.opensource.org/licenses/mit-license.php.

package org.veriblock.miners.pop.api.model

import com.papsign.ktor.openapigen.annotations.Request

@Request("Mine request, optionally specify the block number")
class MineRequest(
    val block: Int?
)

@Request("Withdraw request for the given amount to the given destination address")
class WithdrawRequest(
    var destinationAddress: String?,
    var amount: String?
)

@Request("Configuration change request")
class SetConfigRequest(
    val key: String?,
    val value: String?
)


@Request("Auto mine configuration request")
data class AutoMineConfigRequest(
    val round1: Boolean?,
    val round2: Boolean?,
    val round3: Boolean?,
    val round4: Boolean?
)

@Request("Btc fee configuration request")
class BtcFeeConfigRequest(
    val maxFee: Long?,
    val feePerKB: Long?
)
