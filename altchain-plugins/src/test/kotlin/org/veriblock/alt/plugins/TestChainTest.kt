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

/**
 * Integration tests for development purposes
 */
class TestChainTest {

    @Test
    @Ignore
    fun getBestBlockHeight() {
        val data = TestChain().getBestBlockHeight()
        println(data)
    }

    @Test
    @Ignore
    fun getPublicationData() {
        val data = TestChain().getPublicationData(null)
        println(data)
    }
}
