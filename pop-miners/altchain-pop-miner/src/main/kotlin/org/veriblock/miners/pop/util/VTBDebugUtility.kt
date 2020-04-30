// VeriBlock Blockchain Project
// Copyright 2017-2018 VeriBlock, Inc
// Copyright 2018-2020 Xenios SEZC
// All rights reserved.
// https://www.veriblock.org
// Distributed under the MIT software license, see the accompanying
// file LICENSE or http://www.opensource.org/licenses/mit-license.php.

package org.veriblock.miners.pop.util

import org.veriblock.core.utilities.Utility
import org.veriblock.sdk.models.BitcoinBlock
import org.veriblock.sdk.models.VeriBlockPoPTransaction
import org.veriblock.sdk.models.VeriBlockPublication

/**
 * Note: no methods in this class are to be used in consensus-related code; these methods are strictly for debugging
 * and do not ensure the validity of the data they return (pass in bad data, get bad data back rather than an
 * exception).
 */
object VTBDebugUtility {
    fun vtbConnectsToBtcContext(contextBTCHeaderHashes: List<ByteArray>, vtb: VeriBlockPublication): Boolean {
        val vbkPopTx = vtb.transaction

        val btcBlocksInPoPTx = extractOrderedBtcBlocksFromPopTransaction(vbkPopTx)

        // For all BTC blocks in the PoP transaction, check if any of them claim any of the context BTC headers
        // as their previous block or as themselves.
        return contextBTCHeaderHashes.any { contextBtcHeaderHash ->
            btcBlocksInPoPTx.any { btcBlockInPopTx ->
                Utility.byteArraysAreEqual(contextBtcHeaderHash, btcBlockInPopTx.hash.bytes) ||
                    Utility.byteArraysAreEqual(contextBtcHeaderHash, btcBlockInPopTx.previousBlock.bytes)
            }
        }
    }

    fun doVtbsConnect(oldVtb: VeriBlockPublication, newVtb: VeriBlockPublication, previousVTBs: List<VeriBlockPublication>): Boolean {
        val oldVtbBtcBlocks = extractOrderedBtcBlocksFromPopTransaction(oldVtb.transaction)
        val newVtbBtcBlocks = extractOrderedBtcBlocksFromPopTransaction(newVtb.transaction)
        val directConnection : Boolean
        // Look for a spot where the BTC blocks of oldVtb and newVtb either overlap (both contain at least one header
        // in common) or where one of the BTC blocks in newVtb references one of the BTC blocks in oldVtb as the
        // previous block hash.
        directConnection = oldVtbBtcBlocks.any { oldVtbBtcBlock ->
            newVtbBtcBlocks.any { newVtbBtcBlock ->
                Utility.byteArraysAreEqual(oldVtbBtcBlock.hash.bytes, newVtbBtcBlock.hash.bytes) ||
                    Utility.byteArraysAreEqual(oldVtbBtcBlock.hash.bytes, newVtbBtcBlock.previousBlock.bytes)
            }
        }
        return if (!directConnection) {
            val oldList = previousVTBs.flatMap {
                extractOrderedBtcBlocksFromPopTransaction(it.transaction)
            }
            oldList.any { oldVtbBtcBlock ->
                newVtbBtcBlocks.any { newVtbBtcBlock ->
                    Utility.byteArraysAreEqual(oldVtbBtcBlock.hash.bytes, newVtbBtcBlock.hash.bytes) ||
                        Utility.byteArraysAreEqual(oldVtbBtcBlock.hash.bytes, newVtbBtcBlock.previousBlock.bytes)
                }
            }
        } else {
            directConnection // True
        }
    }

    fun extractOrderedBtcBlocksFromPopTransaction(vbkPopTx: VeriBlockPoPTransaction): List<BitcoinBlock> {
        return vbkPopTx.blockOfProofContext +
            // Block of proof is after the context blocks
            vbkPopTx.blockOfProof
    }

    fun serializeBitcoinBlockHashList(bitcoinBlocks: List<BitcoinBlock>) =
        bitcoinBlocks.joinToString("\n") { it.hash.toString() }
}
