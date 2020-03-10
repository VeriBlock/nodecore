// VeriBlock PoP Miner
// Copyright 2017-2019 Xenios SEZC
// All rights reserved.
// https://www.veriblock.org
// Distributed under the MIT software license, see the accompanying
// file LICENSE or http://www.opensource.org/licenses/mit-license.php.
package nodecore.miners.pop.contracts.result

class IllegalConfigurationValueResultMessage(details: String) : ResultMessage {
    override val code: String
        get() = "V051"

    override val message: String
        get() = "Illegal configuration value"

    override val isError: Boolean
        get() = true

    override val details: List<String> = listOf(details)
}
