// VeriBlock Blockchain Project
// Copyright 2017-2018 VeriBlock, Inc
// Copyright 2018-2020 Xenios SEZC
// All rights reserved.
// https://www.veriblock.org
// Distributed under the MIT software license, see the accompanying
// file LICENSE or http://www.opensource.org/licenses/mit-license.php.

package org.veriblock.lite.core

class MerkleTreeTests {
    /*@Test
    fun proof_WhenMultipleTransactions() {
        val block = FullBlock(
            Utils.decodeHex("000000B20002FEC8639EC1C4FA8004720EF3798A969AE90D4739FE3621F2A86B824905594F5E2CFF4A3F1595AA5531E4F17E65CD5CEEDA100400989602F8459B"),
            listOf(
                VeriBlockTransaction(
                    0x01.toByte(),
                    Address("VGoRtpGVhs875KLPg7CMK2A1TGgBAJ"),
                    Coin.valueOf(8250078432L),
                    listOf(
                        Output.of("V3XANSTH5XNHmXkqrZARCZ1GXAQH86", 165001568L),
                        Output.of("VFMJSUgJCy9QRa1RjXNmJ5kLy5D35C", 8084920000L)
                    ),
                    150L,
                    null,
                    Utils.decodeHex("3046022100AB2D5460E96E248C543BCB49F56E1124D2C35A7B790AE7995C9565300341F9E1022100897B14297C880ADFB3D9A316F352F6BF84650ECC6B49FB75D712701743263E67"),
                    Utils.decodeHex("3056301006072A8648CE3D020106052B8104000A0342000479F3C44CB2AAE1B44AE370F496EBBC00259E0FD7106F60296EF6C08A6D4611941EACFFB151DF84546A2A94083A4C6B6F5573A0CEF7982AF5215573F855C1C8F0"),
                    0xAA.toByte()
                ),
                VeriBlockTransaction(
                    0x01.toByte(),
                    Address("VGoRtpGVhs875KLPg7CMK2A1TGgBAJ"),
                    Coin.valueOf(8250000000L),
                    Arrays.asList(
                        Output.of("V3XANSTH5XNHmXkqrZARCZ1GXAQH86", 165000000L),
                        Output.of("VFMJSUgJCy9QRa1RjXNmJ5kLy5D35C", 8084843136L)
                    ),
                    151L,
                    null,
                    Utils.decodeHex("30450220768AB8C5050E06CE1E78637A1888C77B7AACA3816297FABB10B5263D32D2E72A022100DD3261A7A22307916148712360118833614E6034D1A977BBE0144B671DD19688"),
                    Utils.decodeHex("3056301006072A8648CE3D020106052B8104000A0342000479F3C44CB2AAE1B44AE370F496EBBC00259E0FD7106F60296EF6C08A6D4611941EACFFB151DF84546A2A94083A4C6B6F5573A0CEF7982AF5215573F855C1C8F0"),
                    0xAA.toByte()
                ),
                VeriBlockTransaction(
                    0x01.toByte(),
                    Address("VFMJSUgJCy9QRa1RjXNmJ5kLy5D35C"),
                    Coin.valueOf(57190L),
                    emptyList(),
                    3L,
                    null,
                    Utils.decodeHex("3045022100C3F71607715AA0FA76E8196E0789B0CC7100385EA1E4D2C2230017FA128324A802201D1765E10DF07FAA965DA334419D8152366A686DCE6537567ED375204A8D17BC"),
                    Utils.decodeHex("3056301006072A8648CE3D020106052B8104000A03420004307745C646D2CBDEB06C94D83FEF094C28119AD0037F45D666FF054D15CB160C831FD8103FC2BF46723D9727F05CC4CDFEF166D56C6C17A7489AE68341FD217E"),
                    0xAA.toByte()
                )
            ),
            emptyList(),
            BlockMetaPackage(Sha256Hash.wrap("2EE423B6BA5B1BE877919DB1F16ED833C3B3095CBE95E65EB6844B35817F0F1D"))
        )

        val tree = MerkleTree.of(block)
        val proof = tree.getMerklePath(Sha256Hash.wrap("714CDDB2AE77D963B48258DED5622D861FBA78372209155EC781F1E82A5F91EE"), 2, true)
        Assert.assertEquals(block.merkleRoot, proof.merkleRoot.trim(Sha256Hash.VERIBLOCK_MERKLE_ROOT_LENGTH))
    }

    @Test
    fun proof_WhenSingleTransactions() {
        val block = FullBlock(
            Utils.decodeHex("000000D90002BB59E3262169ACA93987228A2576312CDE0A0ECA9230E84BB85A257403D56AA47826569AAF9BAD831A069E4910485CF01EDD0400989600C114DE"),
            listOf(
                VeriBlockTransaction(
                    0x01.toByte(),
                    Address("VFMJSUgJCy9QRa1RjXNmJ5kLy5D35C"),
                    Coin.valueOf(57190L),
                    emptyList(),
                    9L, null,
                    Utils.decodeHex("3045022024246C01D353876CF8949E2A820B6B499C10DB579FC74BF3236F07D60BCEE656022100F87D68F940BAF84FDC6877826B70C5636788F32E8A14CA5FBEF45941F40A2611"),
                    Utils.decodeHex("3056301006072A8648CE3D020106052B8104000A03420004307745C646D2CBDEB06C94D83FEF094C28119AD0037F45D666FF054D15CB160C831FD8103FC2BF46723D9727F05CC4CDFEF166D56C6C17A7489AE68341FD217E"),
                    0xAA.toByte()
                )
            ),
            emptyList(),
            BlockMetaPackage(Sha256Hash.wrap("F104886044635416387482387D10D074A7AD6E3C474688ADAE85CF8C4D588C38"))
        )

        val tree = MerkleTree.of(block)
        val proof = tree.getMerklePath(Sha256Hash.wrap("7898EFAA2BF5E2783C9DAF2B62883827D3C2D1BABEC101DD622C42F47D63154A"), 0, true)
        Assert.assertEquals(block.merkleRoot, proof.merkleRoot.trim(Sha256Hash.VERIBLOCK_MERKLE_ROOT_LENGTH))
    }*/
}
