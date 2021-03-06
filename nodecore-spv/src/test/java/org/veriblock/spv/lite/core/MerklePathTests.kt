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
import org.veriblock.core.crypto.asBtcHash
import org.veriblock.core.crypto.asMerkleRoot
import org.veriblock.core.crypto.asSha256Hash
import org.veriblock.spv.model.MerkleBranch

class MerklePathTests {

    @Test
    fun verify() {
        val subject = "C929570584BEB05F69B974ABAA5E742FC37AC0001F35E0EBB0B8C8E2FEBFB418".asMerkleRoot()
        val path = listOf(
            "0000000000000000000000000000000000000000000000000000000000000000".asSha256Hash(),
            "953855AD90E483B0492E812CD0581D82F0514675732C24D054AC473EB689D110".asSha256Hash()
        )
        val merklePath = MerkleBranch(0, subject, path, true)
        merklePath.verify("9610685BCE24913B2C8562A95A8A4248B216B3ECA4F6D6D0".asSha256Hash()) shouldBe true
    }

    @Test
    fun verifyWhenFourRegularTransactionsAndPositionThree() {
        val subject = "C929570584BEB05F69B974ABAA5E742FC37AC0001F35E0EBB0B8C8E2FEBFB418".asMerkleRoot()
        val path = listOf(
            "F8D22055FDADA935C8977DF3105E014C4E5862EA2FA3E0C37805D00445377932".asSha256Hash(),
            "2328CD579BFAA48229689CAEC41504FC510C81E458FE9097466A728B5390E0DE".asSha256Hash(),
            "0000000000000000000000000000000000000000000000000000000000000000".asSha256Hash(),
            "953855AD90E483B0492E812CD0581D82F0514675732C24D054AC473EB689D110".asSha256Hash()
        )
        val merklePath = MerkleBranch(3, subject, path, true)
        merklePath.verify("9D5EC2CEB02657B9B2A74A188FE6C593BABE07B297B99F200DFD065906D35795".asSha256Hash()) shouldBe true
    }

    @Test
    fun verifyWhenFourRegularTransactionsAndPositionTwo() {
        val subject = "C929570584BEB05F69B974ABAA5E742FC37AC0001F35E0EBB0B8C8E2FEBFB418".asMerkleRoot()
        val path = listOf(
            "F8D22055FDADA935C8977DF3105E014C4E5862EA2FA3E0C37805D00445377932".asSha256Hash(),
            "2328CD579BFAA48229689CAEC41504FC510C81E458FE9097466A728B5390E0DE".asSha256Hash(),
            "0000000000000000000000000000000000000000000000000000000000000000".asSha256Hash(),
            "953855AD90E483B0492E812CD0581D82F0514675732C24D054AC473EB689D110".asSha256Hash()
        )
        val merklePath = MerkleBranch(2, subject, path, true)
        merklePath.verify("9BBAE0AACC6DD72146C5612B3C625208AFD27EB7F61A3A0FB7D660CA63EF5671".asSha256Hash()) shouldBe true
    }
}
