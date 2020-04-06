// VeriBlock PoP Miner
// Copyright 2017-2020 Xenios SEZC
// All rights reserved.
// https://www.veriblock.org
// Distributed under the MIT software license, see the accompanying
// file LICENSE or http://www.opensource.org/licenses/mit-license.php.
package org.veriblock.miners.pop.model.result

class MissingConfigurationValueResultMessage(private val propertyName: String) : ResultMessage {
    override val code: String
        get() = "V050"

    override val message: String
        get() = "Missing configuration value"

    override val details: List<String>
        get() = listOf("A value is required for property '$propertyName'")

    override val isError: Boolean
        get() = true

}
