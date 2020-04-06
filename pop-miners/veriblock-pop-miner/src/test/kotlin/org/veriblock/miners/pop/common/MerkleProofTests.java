// VeriBlock NodeCore
// Copyright 2017-2020 Xenios SEZC
// All rights reserved.
// https://www.veriblock.org
// Distributed under the MIT software license, see the accompanying
// file LICENSE or http://www.opensource.org/licenses/mit-license.php.

package org.veriblock.miners.pop.common;

import org.bitcoinj.core.PartialMerkleTree;
import org.bitcoinj.core.Sha256Hash;
import org.bitcoinj.params.TestNet3Params;
import org.junit.Assert;
import org.junit.Test;
import org.veriblock.miners.pop.model.merkle.BitcoinMerklePath;
import org.veriblock.miners.pop.model.merkle.BitcoinMerkleTree;
import org.veriblock.miners.pop.model.merkle.MerkleProof;

import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;

public class MerkleProofTests {
    @Test
    public void multipleTransactionsInBlock() {
        byte[] bits = new byte[3];
        bits[0] = -9;
        bits[1] = 14;
        bits[2] = 0;

        List<Sha256Hash> hashes = new LinkedList<>();
        hashes.add(0, Sha256Hash.wrap("ea1a84f5e589ff3a08c65676600df6e800ee8b83c9e27a6360c42a8552248b5a"));
        hashes.add(1, Sha256Hash.wrap("13eb190aaed9b528afc98d37b0675a7c70297c2439816f924d6d86b2672fe738"));
        hashes.add(2, Sha256Hash.wrap("56cac4dc5e9ba53e467ec8f8c5623fc16ee40da16028ebe8295dcc45efb164f4"));
        hashes.add(3, Sha256Hash.wrap("ed1db9c0a208d85da3bddf9094a80f5bbb90a7550be8adf1d09adcf51a79ea11"));
        hashes.add(4, Sha256Hash.wrap("1e84fae6e276fc4ab63000babc0e80431e58b5d05d961100bf68aa919afda5b5"));
        hashes.add(5, Sha256Hash.wrap("c0bd32798e349d592cb2631348c3c2b2bc9bbfe3b64b4d5b1a6d5a8c2c31f4e5"));
        hashes.add(6, Sha256Hash.wrap("93a2ddf5da3b652d7683cb0937e1ed2787436af5fede6664af862e74e6630782"));
        hashes.add(7, Sha256Hash.wrap("f94bea63ca5597af795b101dc5591e4149a4acd2ffc93c5b932649e6a3d9fb23"));
        hashes.add(8, Sha256Hash.wrap("ed95a4ba0f8431b3edaaaf0712c2d70a69cffb0bda5fe38418b9ea2ed2b4911b"));

        PartialMerkleTree tree = new PartialMerkleTree(TestNet3Params.get(), bits, hashes, 68);
        tree.getTxnHashAndMerkleRoot(new LinkedList<>());

        MerkleProof proof = MerkleProof.parse(tree);
        if (proof == null) {
            Assert.fail();
        }

        String txProof = proof.getCompactPath(Sha256Hash.wrap("56cac4dc5e9ba53e467ec8f8c5623fc16ee40da16028ebe8295dcc45efb164f4"));
        String[] parts = txProof.split(":");

        String[] expected = new String[]{
            "17", "F464B1EF45CC5D29E8EB2860A10DE46EC13F62C5F8C87E463EA59B5EDCC4CA56",
            "38E72F67B2866D4D926F8139247C29707C5A67B0378DC9AF28B5D9AE0A19EB13",
            "C40E9E844481BB835892BDDCDE0402C2091746AAAE2BFBE7CF33EF4B959B91BE",
            "E5F4312C8C5A6D1A5B4D4BB6E3BF9BBCB2C2C3481363B22C599D348E7932BDC0",
            "820763E6742E86AF6466DEFEF56A438727EDE13709CB83762D653BDAF5DDA293",
            "5A8B2452852AC460637AE2C9838BEE00E8F60D607656C6083AFF89E5F5841AEA",
            "23FBD9A3E64926935B3CC9FFD2ACA449411E59C51D105B79AF9755CA63EA4BF9",
            "1B91B4D22EEAB91884E35FDA0BFBCF690AD7C21207AFAAEDB331840FBAA495ED"
        };

        Assert.assertArrayEquals(expected, parts);

        BitcoinMerklePath verification = new BitcoinMerklePath(txProof);
        String calculatedRoot = verification.getMerkleRoot();

        Assert.assertEquals("bdd18f2b0ebacaa27e39cf74e84f4db75447dc77da11fd2c87dc67aea5b9bb96", calculatedRoot.toLowerCase());
        Assert.assertEquals(
            "bdd18f2b0ebacaa27e39cf74e84f4db75447dc77da11fd2c87dc67aea5b9bb96",
            tree.getTxnHashAndMerkleRoot(new LinkedList<>()).toString()
        );
    }

    @Test
    public void old() {
        String[] txns = new String[]{
            "df48199e864d5c38feb0482531d0f74530946df1d0a05d6be4f79be89c3bc125",
            "18565166b4d2d6923a3e7729383593080e4e6d41deb473fe45de8381dccc617c",
            "c44d996f7f5b2f309bd527b581156389b2e2eb9237bf9eae35a6956e24c03ab0",
            "994a25c7a2cb27450cfac3023cd5dba15b5d5b02655e76c1674d7af82588bec9",
            "a5dfb01b35a4f5e36f77855ee9305e80203257b38f10b9ec6177d3a4877d134f",
            "76eef77565968aeb94aa6392522f19d33b52576d1593f40eeb859894172cd83a",
            "f213ad76c26893b2831126b709d3c1d0980fd68312d8627164f47d7100814450",
            "22841ef8fcb466239865a3e0ff1bbc426cd4aa0d30ff68e43de0d734170194c7",
            "9ef87b962dda56e372b14af49c9a89309e2e89ee4db2dff12fffecb7dd20663f",
            "c82757d72b9b0a056c4e39727765d517de45d7d23a0c574787c496e0c79af4c9",
            "b2c2dbe83928738f738ef2170fcdf3af35696c1fc6ba1bcab4825234b26573af",
            "f3e848ea97b720c00e746537f7f7a3ca0b468db3cabcf6963c29e5b4f50fa45f",
            "ebc4858ead48c850a9a3df9412f90b8a2a4657a13fdbb56db9eef4764b5c1c98",
            "1057ce6098bfad74da7bc6001be42e6e660f8b29c4495c074b712d47d9e33ca6",
            "11e47aba8052e80c397fdc20c6375aaaa1717858c8ebb8a925fb8bbe762e517b",
            "7ed4875f7ca796a46491cbecb71f9d84da2ecc226912e9490b9dd5ee5eb42d02",
            "13eb190aaed9b528afc98d37b0675a7c70297c2439816f924d6d86b2672fe738",
            "56cac4dc5e9ba53e467ec8f8c5623fc16ee40da16028ebe8295dcc45efb164f4",
            "ed1db9c0a208d85da3bddf9094a80f5bbb90a7550be8adf1d09adcf51a79ea11",
            "1e84fae6e276fc4ab63000babc0e80431e58b5d05d961100bf68aa919afda5b5",
            "8d0f203c1742a019c788c95f6e55c02ffd37d10da60c8591914e67cba76720d9",
            "a0520c5bf03b316229b14458a03c93ea295d9db9c0985ec6b3766a187cfbae30",
            "5d87fea70feb581f69071efcad0c1f4d813edd8abb7bbdb71a3ab082e98c516b",
            "56f96d14bf113b84158e2ba6c43d18e48933c889b49cb67426a3c452a0dbbd74",
            "260113f61ea0e5f0c52905506036b68680b6e289692e45ebb53bfb71f43136f2",
            "5af2ee6ae4cd725648c3f29ba5ded12c742035b74ea968afc16b3c9f6e6dce13",
            "882c4370b4cba7761cdc9a39c1462bf3dc10beea560b18d153e04b9f69ba3054",
            "1fcc2b69afd9635eb7996d430a94ee9c56462054c44f851a4b9c5cc25d958599",
            "1bc88ee3919d855b52d184ff7c3cdc2d64411826b0b43c482602a0839a984d2f",
            "4fe4b7dce152666b5cdfc06a4ab03bfe41cddc2b29ba365e295de3d2214a7ea5",
            "33bb003efe53bbfa0c32f70e8c7ee8ff057dc9bc5d9935f8c294f41a309c8bfb",
            "0e8fd9bd8646fa3f81c4214e826a5775fd37936d8a590f33bc885754a9724838",
            "d2b024cb40919e11a25e40fa471273e9e3a9c360fc4d6c45019d48a0616e235c",
            "be23e3465d77c2afd9ecd9d02930c55660fcde2bd9ed2e65fdef723aa1899128",
            "2d47a8f863e4a44fcd478d566b7b01f4db431ff3a3cdfe5cb7bd4c7221602f87",
            "ff62992954cce7c99be42e845100365cf866a97e685effd5dbc8b18da7b6bcf7",
            "20c9b23bbacb0cbc9bba6c912d466589165f9b328896227809bdc0d2776283a2",
            "0ce54d163998e6e09f2f19f2a411e66640b011bda5fbab6f7bf9e9aa3a3a0608",
            "5f7e6ac3cc1254a99a1757f91adfca600c29ea0f0496ea0a09aa67fed505ad1e",
            "90f47999fb972d62a46317656d6e28bbd1f150d1edecbe3120ff3f011369feee",
            "1e5284c141af9df4b226e1aa8717414041feeb6a984b57503b4e8d62621a1b09",
            "89220a7ef79681bfcb02b10d4f3a473d9fdfa0383185967e5818788d6e46efe8",
            "f85c0525becaf7049dd08e1981dcc193aae3bd0174262dcd430d59fdd11546fb",
            "a7e123459097f3e51523c277a07fa07f52f5e3c17b6e2d159132f72806af941c",
            "024ebfd1739e908b2ff8054467030eb07c6570c8a8b030c86daa77323d9a2bb9",
            "83ad6ffc117f01d0825b29c95e296414368db9fdcbcbfc09704f576e88406298",
            "fc6d8a9b49c4b2c8c22351edb0b01849117293a6ded1e43627636ef9c47b750b",
            "7018188bd20c571254df6784c5f8d42365734fdf585e5169559d38c7947e46c3",
            "5361ccb5448f17de370a670ccac45181e070024e2672ac37e1f76528f75fae8d",
            "51f4bd007c9ef85dd4f2fa5d7a0e4ecda21069db7da9993638a7e109600bb2de",
            "3fd09ad97c2fe3b46f7ac4143b94dd1b64e8a452fa9bed8fe759324d823d1af9",
            "b07d9a415e2d21dcc04ceb442f10c227ec2c2cbb2d9523b337dc3b28f95a9c81",
            "554007d612c55fd8c510fd09a1c1bffa7bc78b93ee2e0de1701ff584cb5c896b",
            "551efdac0617ce7f6cc518c7cef24d45a273d97ccacb13df1f66ad953d166950",
            "dfa3aac12863c2acdcc6eaf548692fb8842d00ca251b93e7b394afcd47d2f354",
            "ef3cacbc933b9339a17b0b35ed44671e922dd5c45e0ff1187cc18c94575daa0e",
            "a070f6878e5efec7d33a04664b5b99ffea7b9b46537b52639eae8e332d056a0d",
            "cf23ae4539cb1197cdec19b39c94920655687387d9f5cef80dbe4961e37bb223",
            "17c997aa1f6f7f29db6d9ef6717f2c6e3b04b854e6869002be11a7b5721c6230",
            "8135be6eb6096474b0b834f895470761db9a3f6602b637b61133675377ecee37",
            "c555e618658e6e253b3d4c42db81404b6333d6e4db83c4433c70600498185ee3",
            "86ab4d0476902c7f4ad840fd8f818031446016d47747772e88cb0d94cae1921c",
            "8d3fd0a7d74a0e5694ee19f1a9c51574d7ba90f788102fa2aa627f9d60868851",
            "d7f528a2f67dbee211742adee006676c70c5f615c868eea9b00f6f42e9e0a7eb",
            "48e21a989d896529ee48e678f455dd77403636b16c0d1eadeef5b18bccdda117",
            "6d3b73da45161098b2847fd09e64b84421d887536ff3592bf969b293a3348fe3",
            "b751b89ab152492059e242a064c5dcf85d5c9412fe24d392f030c134dc1c1cdf",
            "982c260e95b29fc8d11fda14421878069cea000ed317a552f13ae18e5596db42"
        };
        BitcoinMerkleTree tree = new BitcoinMerkleTree(true, Arrays.asList(txns));
        BitcoinMerklePath path = tree.getPathFromTxID("56cac4dc5e9ba53e467ec8f8c5623fc16ee40da16028ebe8295dcc45efb164f4");

        String[] parts = path.getCompactFormat().split(":");

        String[] expected = new String[]{
            "17", "F464B1EF45CC5D29E8EB2860A10DE46EC13F62C5F8C87E463EA59B5EDCC4CA56",
            "38E72F67B2866D4D926F8139247C29707C5A67B0378DC9AF28B5D9AE0A19EB13",
            "C40E9E844481BB835892BDDCDE0402C2091746AAAE2BFBE7CF33EF4B959B91BE",
            "E5F4312C8C5A6D1A5B4D4BB6E3BF9BBCB2C2C3481363B22C599D348E7932BDC0",
            "820763E6742E86AF6466DEFEF56A438727EDE13709CB83762D653BDAF5DDA293",
            "5A8B2452852AC460637AE2C9838BEE00E8F60D607656C6083AFF89E5F5841AEA",
            "23FBD9A3E64926935B3CC9FFD2ACA449411E59C51D105B79AF9755CA63EA4BF9",
            "1B91B4D22EEAB91884E35FDA0BFBCF690AD7C21207AFAAEDB331840FBAA495ED"
        };

        Assert.assertArrayEquals(expected, parts);

        Assert.assertEquals("bdd18f2b0ebacaa27e39cf74e84f4db75447dc77da11fd2c87dc67aea5b9bb96", path.getMerkleRoot().toLowerCase());
    }
}
