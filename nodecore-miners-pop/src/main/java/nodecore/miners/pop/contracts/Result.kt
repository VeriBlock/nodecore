// VeriBlock PoP Miner
// Copyright 2017-2019 Xenios SEZC
// All rights reserved.
// https://www.veriblock.org
// Distributed under the MIT software license, see the accompanying
// file LICENSE or http://www.opensource.org/licenses/mit-license.php.

package nodecore.miners.pop.contracts

import java.util.*

open class Result {
    private var failed = false
    val messages: ArrayList<ResultMessage> = ArrayList()

    fun fail() {
        failed = true
    }

    fun didFail(): Boolean {
        return failed
    }

    fun addMessage(code: String?, message: String?, details: String?, error: Boolean) {
        messages.add(DefaultResultMessage(code, message, details, error))
    }

    fun addMessage(resultMessage: ResultMessage) {
        messages.add(resultMessage)
    }
}
