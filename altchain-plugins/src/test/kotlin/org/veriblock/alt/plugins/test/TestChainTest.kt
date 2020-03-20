// VeriBlock Blockchain Project
// Copyright 2017-2018 VeriBlock, Inc
// Copyright 2018-2020 Xenios SEZC
// All rights reserved.
// https://www.veriblock.org
// Distributed under the MIT software license, see the accompanying
// file LICENSE or http://www.opensource.org/licenses/mit-license.php.

package org.veriblock.alt.plugins.test

import org.junit.Ignore
import org.junit.Test
import org.veriblock.sdk.alt.plugin.PluginConfig

/**
 * Integration tests for development purposes
 */
class TestChainTest {

    private val chain = TestChain(
        "test",
        PluginConfig()
    )

    @Test
    @Ignore
    fun getBestBlockHeight() {
        val data = chain.getBestBlockHeight()
        println(data)
    }

    @Test
    @Ignore
    fun getPublicationData() {
        val data = chain.getMiningInstruction(null)
        println(data)
    }
}
