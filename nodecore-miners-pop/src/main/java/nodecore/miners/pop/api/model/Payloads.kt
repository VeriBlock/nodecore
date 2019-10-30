// VeriBlock NodeCore
// Copyright 2017-2019 Xenios SEZC
// All rights reserved.
// https://www.veriblock.org
// Distributed under the MIT software license, see the accompanying
// file LICENSE or http://www.opensource.org/licenses/mit-license.php.

package nodecore.miners.pop.api.model

class MineRequestPayload {
    var block: Int? = null
}

class SetConfigRequestPayload {
    var key: String? = null
    var value: String? = null
}
