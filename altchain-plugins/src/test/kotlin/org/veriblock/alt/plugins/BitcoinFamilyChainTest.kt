// VeriBlock Blockchain Project
// Copyright 2017-2018 VeriBlock, Inc
// Copyright 2018-2019 Xenios SEZC
// All rights reserved.
// https://www.veriblock.org
// Distributed under the MIT software license, see the accompanying
// file LICENSE or http://www.opensource.org/licenses/mit-license.php.

package org.veriblock.alt.plugins

import org.junit.Ignore
import org.junit.Test
import org.veriblock.core.utilities.extensions.asHexBytes

/**
 * Integration tests for development purposes
 */
@Ignore
class BitcoinFamilyChainTest {

    @Test
    fun getBestBlockHeight() {
        val data = BitcoinFamilyChain(0, "btc", "Bitcoin").getBestBlockHeight()
        println(data)
    }

    @Test
    fun getBlockByHash() {
        val data = BitcoinFamilyChain(0, "btc", "Bitcoin").getBlock("5d29bf53ee5840979b1601219666ece6b5a41d35b4602e342959b89c80218dea")
        println(data)
    }

    @Test
    fun getBlockByHeight() {
        val data = BitcoinFamilyChain(0, "btc", "Bitcoin").getBlock(223)
        println(data)
    }

    @Test
    fun checkBlockIsOnMainChain() {
        val data = BitcoinFamilyChain(0, "btc", "Bitcoin").checkBlockIsOnMainChain(223, "000000303c1682d3552038664b5f606f3d8655cb82cf566950cef8c67ba1b2".asHexBytes())
        println(data)
    }

    @Test
    fun getTransaction() {
        val data = BitcoinFamilyChain(0, "btc", "Bitcoin").getTransaction("be3aeeae1428b21ab244180ebca7494a3ae413d50f917e6a227ed2cae36c9503")
        println(data)
    }

    @Test
    fun getPublicationData() {
        val data = BitcoinFamilyChain(0, "btc", "Bitcoin").getPublicationData(null)
        println(data.getDetailedInfo())
    }
}
