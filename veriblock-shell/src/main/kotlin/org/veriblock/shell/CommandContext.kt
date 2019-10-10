// VeriBlock Blockchain Project
// Copyright 2017-2018 VeriBlock, Inc
// Copyright 2018-2019 Xenios SEZC
// All rights reserved.
// https://www.veriblock.org
// Distributed under the MIT software license, see the accompanying
// file LICENSE or http://www.opensource.org/licenses/mit-license.php.

package org.veriblock.shell

class CommandContext(
    private val shell: Shell,
    private val parameters: Map<String, Any>
) {
    var quit = false
        private set
    var clear = false
        private set

    @Suppress("UNCHECKED_CAST")
    fun <T> getParameter(name: String) = parameters[name] as T

    fun quit() {
        quit = true
    }

    fun clear() {
        clear = true
    }

    fun printInfo(message: String) {
        shell.printInfo(message)
    }
}
