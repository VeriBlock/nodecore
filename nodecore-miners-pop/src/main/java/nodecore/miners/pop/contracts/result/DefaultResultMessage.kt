// VeriBlock PoP Miner
// Copyright 2017-2019 Xenios SEZC
// All rights reserved.
// https://www.veriblock.org
// Distributed under the MIT software license, see the accompanying
// file LICENSE or http://www.opensource.org/licenses/mit-license.php.
package nodecore.miners.pop.contracts.result

class DefaultResultMessage : ResultMessage {
    override val code: String
    override val message: String
    override val details: List<String>
    override val isError: Boolean

    constructor(code: String, message: String, details: String, error: Boolean) {
        this.code = code
        this.message = message
        this.details = listOf(details)
        isError = error
    }

    constructor(code: String, message: String, details: List<String>, error: Boolean) {
        this.code = code
        this.message = message
        this.details = details
        isError = error
    }
}
