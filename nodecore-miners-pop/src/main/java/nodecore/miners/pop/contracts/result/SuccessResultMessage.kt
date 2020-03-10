// VeriBlock PoP Miner
// Copyright 2017-2019 Xenios SEZC
// All rights reserved.
// https://www.veriblock.org
// Distributed under the MIT software license, see the accompanying
// file LICENSE or http://www.opensource.org/licenses/mit-license.php.
package nodecore.miners.pop.contracts.result

class SuccessResultMessage : ResultMessage {
    override val code: String
        get() = "V200"

    override val message: String
        get() = "Success"

    override val details: List<String>
        get() = emptyList()

    override val isError: Boolean
        get() = false
}
