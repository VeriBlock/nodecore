// VeriBlock Blockchain Project
// Copyright 2017-2018 VeriBlock, Inc
// Copyright 2018-2020 Xenios SEZC
// All rights reserved.
// https://www.veriblock.org
// Distributed under the MIT software license, see the accompanying
// file LICENSE or http://www.opensource.org/licenses/mit-license.php.

package org.veriblock.shell.core

class ResultMessage(
    val code: String,
    val message: String,
    val details: List<String> = emptyList(),
    val isError: Boolean = false
) {
    constructor(code: String, message: String, details: String, error: Boolean)
        : this(code, message, listOf(details), error)
}
