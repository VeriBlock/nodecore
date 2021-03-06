// VeriBlock Blockchain Project
// Copyright 2017-2018 VeriBlock, Inc
// Copyright 2018-2021 Xenios SEZC
// All rights reserved.
// https://www.veriblock.org
// Distributed under the MIT software license, see the accompanying
// file LICENSE or http://www.opensource.org/licenses/mit-license.php.
package org.veriblock.spv.lite.core

import io.kotest.matchers.shouldBe
import org.junit.Test
import org.veriblock.core.Context
import org.veriblock.core.crypto.VBlake
import org.veriblock.core.utilities.BlockUtility
import org.veriblock.core.params.defaultTestNetParameters
import org.veriblock.core.utilities.extensions.toHex

class VBlakeHashTests {

    @Test
    fun hash() {
        Context.create(defaultTestNetParameters)
        val header = BlockUtility.assembleBlockHeader(
            14,
            1.toShort(),
            "000041E5DA03789160522C40829F51AE9497CB1274FCD002",
            "00008A23FE9C7B8EDC7210C37B6242D998254DA0643B831F",
            "000000000000000000000000000000000000000000000000",
            "481DB874D6AD57556549672C101D83677BDAC6508D5AA843",
            1539117202,
            50431648,
            53011
        )
        val hash = VBlake.hash(header).toHex()
        hash shouldBe "000060CB002FB9F2A1F6CAB0662FE96521138AD1FF6ABB89"
    }
}
