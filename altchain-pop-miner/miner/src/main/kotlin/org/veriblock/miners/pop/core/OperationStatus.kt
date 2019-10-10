// VeriBlock Blockchain Project
// Copyright 2017-2018 VeriBlock, Inc
// Copyright 2018-2019 Xenios SEZC
// All rights reserved.
// https://www.veriblock.org
// Distributed under the MIT software license, see the accompanying
// file LICENSE or http://www.opensource.org/licenses/mit-license.php.

package org.veriblock.miners.pop.core

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
