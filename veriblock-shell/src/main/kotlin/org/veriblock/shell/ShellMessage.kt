// VeriBlock Blockchain Project
// Copyright 2017-2018 VeriBlock, Inc
// Copyright 2018-2020 Xenios SEZC
// All rights reserved.
// https://www.veriblock.org
// Distributed under the MIT software license, see the accompanying
// file LICENSE or http://www.opensource.org/licenses/mit-license.php.

package org.veriblock.shell

import org.jline.utils.AttributedStyle
import org.veriblock.shell.core.ActivityLevel

class ShellMessage(
    val level: ActivityLevel,
    val message: String,
    val details: List<String> = emptyList()
) {
    fun getColor(): Int = when (level) {
        ActivityLevel.INFO -> AttributedStyle.WHITE
        ActivityLevel.WARN -> AttributedStyle.YELLOW
        ActivityLevel.ERROR -> AttributedStyle.RED
        ActivityLevel.SUCCESS -> AttributedStyle.GREEN
        ActivityLevel.MINER -> AttributedStyle.CYAN
    }
}
