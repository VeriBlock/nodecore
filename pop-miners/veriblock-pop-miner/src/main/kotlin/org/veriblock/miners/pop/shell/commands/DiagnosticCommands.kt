// VeriBlock Blockchain Project
// Copyright 2017-2018 VeriBlock, Inc
// Copyright 2018-2020 Xenios SEZC
// All rights reserved.
// https://www.veriblock.org
// Distributed under the MIT software license, see the accompanying
// file LICENSE or http://www.opensource.org/licenses/mit-license.php.

package org.veriblock.miners.pop.shell.commands

import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeoutOrNull
import org.apache.commons.lang3.tuple.Pair
import org.bitcoinj.core.Utils
import org.veriblock.core.utilities.extensions.toHex
import org.veriblock.miners.pop.service.MinerService
import org.veriblock.shell.CommandFactory
import org.veriblock.shell.command
import org.veriblock.shell.core.failure
import org.veriblock.shell.core.success
import java.io.ByteArrayOutputStream

fun CommandFactory.diagnosticCommands(minerService: MinerService) {
    command(
        name = "Show Last Bitcoin Block",
        form = "showlastbitcoinblock",
        description = "Displays information about the most recent Bitcoin block"
    ) {
        printInfo("Configuration Properties:")

        val lastBlock = minerService.getLastBitcoinBlock()
        val lastBlockHeader = lastBlock.header

        val headerOutputSteram = ByteArrayOutputStream()

        Utils.uint32ToByteStreamLE(lastBlockHeader.version, headerOutputSteram)
        headerOutputSteram.write(lastBlockHeader.prevBlockHash.reversedBytes)
        headerOutputSteram.write(lastBlockHeader.merkleRoot.reversedBytes)
        Utils.uint32ToByteStreamLE(lastBlockHeader.timeSeconds, headerOutputSteram)
        Utils.uint32ToByteStreamLE(lastBlockHeader.difficultyTarget, headerOutputSteram)
        Utils.uint32ToByteStreamLE(lastBlockHeader.nonce, headerOutputSteram)

        printInfo("Bitcoin Block Header: ${headerOutputSteram.toByteArray().toHex()}")
        printInfo("Bitcoin Block Hash: ${lastBlockHeader.hash}")
        printInfo("Bitcoin Block Height: ${lastBlock.height}")

        success {
            addMessage("V200", "Success", headerOutputSteram.toByteArray().toHex());
        }
    }

    command(
        name = "Show Recent Bitcoin Fees",
        form = "showrecentbitcoinfees",
        description = "Returns the average fee per byte in a recent Bitcoin block"
    ) {
        try {
            val blockFees: Pair<Int, Long>? = runBlocking {
                withTimeoutOrNull(5_000L) {
                    minerService.showRecentBitcoinFees()
                }
            }
            if (blockFees != null) {
                printInfo("Bitcoin Block #${blockFees.left} -> Average Fee per Byte: ${blockFees.right}")
                success()
            } else {
                failure {
                    addMessage("V500", "Error", "Unable to fetch fees for recent block")
                }
            }
        } catch (e: Exception) {
            failure {
                addMessage("V500", "Error", e.message!!)
            }
        }
    }
}
