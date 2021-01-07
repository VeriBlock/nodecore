// VeriBlock NodeCore CLI
// Copyright 2017-2020 Xenios SEZC
// All rights reserved.
// https://www.veriblock.org
// Distributed under the MIT software license, see the accompanying
// file LICENSE or http://www.opensource.org/licenses/mit-license.php.
package nodecore.cli.serialization

import nodecore.api.grpc.VeriBlockMessages.BlackListInfo

class BlacklistInfo(
    info: BlackListInfo
) {
    val type = info.type.name

    val value = info.value

    val reason = info.reason

    val timestamp = info.timestamp

    val expiryTimestamp = info.expiryTimestamp
}
