// VeriBlock Blockchain Project
// Copyright 2017-2018 VeriBlock, Inc
// Copyright 2018-2019 Xenios SEZC
// All rights reserved.
// https://www.veriblock.org
// Distributed under the MIT software license, see the accompanying
// file LICENSE or http://www.opensource.org/licenses/mit-license.php.

package org.veriblock.miners.pop.shell.commands

import com.google.gson.Gson
import org.veriblock.miners.pop.Miner
import org.veriblock.shell.Shell

fun Shell.walletCommands(miner: Miner, prettyPrintGson: Gson) {

    //command(
    //    name = "Get Balance",
    //    form = "getbalance",
    //    description = "Gets the coin balance for the VeriBlock address"
    //) {
    //    val balance = miner.balance
    //    if (balance != null) {
    //        printInfo(prettyPrintGson.toJson(balance))
    //    } else {
    //        printInfo("Could not get balance")
    //    }
    //
    //    success()
    //}
}
