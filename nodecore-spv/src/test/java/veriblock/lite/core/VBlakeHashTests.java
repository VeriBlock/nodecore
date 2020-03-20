// VeriBlock Blockchain Project
// Copyright 2017-2018 VeriBlock, Inc
// Copyright 2018-2020 Xenios SEZC
// All rights reserved.
// https://www.veriblock.org
// Distributed under the MIT software license, see the accompanying
// file LICENSE or http://www.opensource.org/licenses/mit-license.php.
package veriblock.lite.core;

import org.junit.Assert;
import org.junit.Test;
import org.veriblock.core.utilities.BlockUtility;
import org.veriblock.sdk.models.VBlakeHash;

public class VBlakeHashTests {
    @Test
    public void hash() {
        byte[] header = BlockUtility.assembleBlockHeader(
                14,
                (short)1,
                "000041E5DA03789160522C40829F51AE9497CB1274FCD002",
                "00008A23FE9C7B8EDC7210C37B6242D998254DA0643B831F",
                "000000000000000000000000000000000000000000000000",
                "481DB874D6AD57556549672C101D83677BDAC6508D5AA843",
                1539117202,
                50431648,
                53011
        );

        VBlakeHash hash = VBlakeHash.hash(header);

        Assert.assertEquals("000060CB002FB9F2A1F6CAB0662FE96521138AD1FF6ABB89", hash.toString());
    }
}
