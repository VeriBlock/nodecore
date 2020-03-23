// VeriBlock PoP Miner
// Copyright 2017-2020 Xenios SEZC
// All rights reserved.
// https://www.veriblock.org
// Distributed under the MIT software license, see the accompanying
// file LICENSE or http://www.opensource.org/licenses/mit-license.php.
package nodecore.miners.pop.core

enum class OperationStatus(
    val value: Int
) {
    RUNNING(1),
    COMPLETED(2),
    FAILED(3),
    UNKNOWN(0);

    companion object {

        fun parse(value: Int): OperationStatus {
            return values().find {
                it.value == value
            } ?: UNKNOWN
        }
    }
}
