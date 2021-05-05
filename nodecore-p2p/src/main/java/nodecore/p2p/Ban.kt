// VeriBlock NodeCore
// Copyright 2017-2021 Xenios SEZC
// All rights reserved.
// https://www.veriblock.org
// Distributed under the MIT software license, see the accompanying
// file LICENSE or http://www.opensource.org/licenses/mit-license.php.
package nodecore.p2p

import org.veriblock.core.utilities.Utility

private const val BAN_DURATION = 60 * 60 * 4L // 4 hours

class Ban(
    val type: Type,
    val address: String,
    val expiration: Long
) {
    enum class Type {
        Temporary, Permanent
    }
    
    val isExpired: Boolean
        get() = type != Type.Permanent && Utility.getCurrentTimeSeconds() > expiration
}

fun createTemporaryBan(address: String): Ban {
    return Ban(Ban.Type.Temporary, address, Utility.getCurrentTimeSeconds() + BAN_DURATION)
}

fun createPermanentBan(address: String): Ban {
    return Ban(Ban.Type.Permanent, address, -1)
}
