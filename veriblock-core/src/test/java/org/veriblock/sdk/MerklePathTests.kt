// VeriBlock Blockchain Project
// Copyright 2017-2018 VeriBlock, Inc
// Copyright 2018-2019 Xenios SEZC
// All rights reserved.
// https://www.veriblock.org
// Distributed under the MIT software license, see the accompanying
// file LICENSE or http://www.opensource.org/licenses/mit-license.php.
package org.veriblock.sdk

import io.kotlintest.shouldBe
import org.junit.Test
import org.veriblock.core.crypto.Sha256Hash
import org.veriblock.core.utilities.Utility
import org.veriblock.sdk.models.MerklePath
import java.nio.ByteBuffer

private const val defaultPathEncoded =
    "02019f040000067b040000000c040000000400000020204d66077fdf24246ffd6b6979dfed" +
        "ef5d46588654addeb35edb11e993c131f61220023d1abe8758c6f917ec0c65674bbd43d66e" +
        "e14dc667b3117dfc44690c6f5af120096ddba03ca952af133fb06307c24171e53bf50ab76f" +
        "1edeabde5e99f78d4ead202f32cf1bee50349d56fc1943af84f2d2abda520f64dc4db37b2f" +
        "3db20b0ecb572093e70120f1b539d0c1495b368061129f30d35f9e436f32d69967ae86031a" +
        "275620f554378a116e2142f9f6315a38b19bd8a1b2e6dc31201f2d37a058f03c39c06c2008" +
        "24705685ceca003c95140434ee9d8bbbf4474b83fd4ecc2766137db9a44d7420b7b9e52f3e" +
        "e8ce4fbb8be7d6cf66d33a20293f806c69385136662a74453fb162201732c9a35e80d4796b" +
        "abea76aace50b49f6079ea3e349f026b4491cfe720ad17202d9b57e92ab51fe28a587050fd" +
        "82abb30abd699a5ce8b54e7cd49b2a827bcb9920dcba229acdc6b7f028ba756fd5abbfebd3" +
        "1b4227cd4137d728ec5ea56c457618202cf1439a6dbcc1a35e96574bddbf2c5db9174af5ad" +
        "0d278fe92e06e4ac349a42"

private const val defaultSubject = "94e097b110ba3adbb7b6c4c599d31d675de7be6e722407410c08ef352be585f1"
private const val defaultMerkleCompact = "1659:94E097B110BA3ADBB7B6C4C599D31D675DE7BE6E722407410C08EF352BE585F1:4D66077FDF24246FFD6B6979DFEDEF5D46588654ADDEB35EDB11E993C131F612:023D1ABE8758C6F917EC0C65674BBD43D66EE14DC667B3117DFC44690C6F5AF1:096DDBA03CA952AF133FB06307C24171E53BF50AB76F1EDEABDE5E99F78D4EAD:2F32CF1BEE50349D56FC1943AF84F2D2ABDA520F64DC4DB37B2F3DB20B0ECB57:93E70120F1B539D0C1495B368061129F30D35F9E436F32D69967AE86031A2756:F554378A116E2142F9F6315A38B19BD8A1B2E6DC31201F2D37A058F03C39C06C:0824705685CECA003C95140434EE9D8BBBF4474B83FD4ECC2766137DB9A44D74:B7B9E52F3EE8CE4FBB8BE7D6CF66D33A20293F806C69385136662A74453FB162:1732C9A35E80D4796BABEA76AACE50B49F6079EA3E349F026B4491CFE720AD17:2D9B57E92AB51FE28A587050FD82ABB30ABD699A5CE8B54E7CD49B2A827BCB99:DCBA229ACDC6B7F028BA756FD5ABBFEBD31B4227CD4137D728EC5EA56C457618:2CF1439A6DBCC1A35E96574BDDBF2C5DB9174AF5AD0D278FE92E06E4AC349A42"

class MerklePathTests {
    @Test
    fun parse() {
        val input = Utility.hexToBytes(defaultPathEncoded)
        val subject = Utility.hexToBytes(defaultSubject)
        val decoded = MerklePath.parse(ByteBuffer.wrap(input), Sha256Hash.wrap(subject))
        decoded.subject shouldBe Sha256Hash.wrap(subject)
        decoded.index shouldBe 1659
    }

    @Test
    fun roundtrip() {
        val input = MerklePath(defaultMerkleCompact)
        val bytes = input.serialize()
        val decoded = MerklePath.parse(ByteBuffer.wrap(bytes), input.subject)
        input shouldBe decoded
    }
}
