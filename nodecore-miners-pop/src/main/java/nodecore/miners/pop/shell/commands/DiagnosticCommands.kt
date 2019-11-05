// VeriBlock Blockchain Project
// Copyright 2017-2018 VeriBlock, Inc
// Copyright 2018-2019 Xenios SEZC
// All rights reserved.
// https://www.veriblock.org
// Distributed under the MIT software license, see the accompanying
// file LICENSE or http://www.opensource.org/licenses/mit-license.php.

package nodecore.miners.pop.shell.commands

import nodecore.miners.pop.common.Utility
import nodecore.miners.pop.PoPMiner
import org.apache.commons.lang3.tuple.Pair
import org.bitcoinj.core.Utils
import org.veriblock.shell.Shell
import org.veriblock.shell.command
import org.veriblock.shell.core.failure
import org.veriblock.shell.core.success
import java.io.ByteArrayOutputStream

fun Shell.diagnosticCommands(miner: PoPMiner) {
    command(
        name = "Show Last Bitcoin Block",
        form = "showlastbitcoinblock",
        description = "Displays information about the most recent Bitcoin block"
    ) {
        printInfo("Configuration Properties:")

        val lastBlock = miner.lastBitcoinBlock
        val lastBlockHeader = lastBlock.header

        val headerOutputSteram = ByteArrayOutputStream()

        Utils.uint32ToByteStreamLE(lastBlockHeader.version, headerOutputSteram)
        headerOutputSteram.write(lastBlockHeader.prevBlockHash.reversedBytes)
        headerOutputSteram.write(lastBlockHeader.merkleRoot.reversedBytes)
        Utils.uint32ToByteStreamLE(lastBlockHeader.timeSeconds, headerOutputSteram)
        Utils.uint32ToByteStreamLE(lastBlockHeader.difficultyTarget, headerOutputSteram)
        Utils.uint32ToByteStreamLE(lastBlockHeader.nonce, headerOutputSteram)

        printInfo("Bitcoin Block Header: ${Utility.bytesToHex(headerOutputSteram.toByteArray())}")
        printInfo("Bitcoin Block Hash: ${lastBlockHeader.hash}")
        printInfo("Bitcoin Block Height: ${lastBlock.height}")

        success {
            addMessage("V200", "Success", Utility.bytesToHex(headerOutputSteram.toByteArray()));
        }
    }

    command(
        name = "Show Recent Bitcoin Fees",
        form = "showrecentbitcoinfees",
        description = "Returns the average fee per byte in a recent Bitcoin block"
    ) {
        try {
            val blockFees: Pair<Int, Long>? = miner.showRecentBitcoinFees()
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
