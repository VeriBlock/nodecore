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
import org.veriblock.sdk.models.Sha256Hash;
import veriblock.model.MerkleBranch;

import java.util.Arrays;
import java.util.List;

public class MerklePathTests {
    @Test
    public void verify() {
        Sha256Hash subject = Sha256Hash.wrap("C929570584BEB05F69B974ABAA5E742FC37AC0001F35E0EBB0B8C8E2FEBFB418");
        List<Sha256Hash> path = Arrays.asList(
                Sha256Hash.wrap("0000000000000000000000000000000000000000000000000000000000000000"),
                Sha256Hash.wrap("953855AD90E483B0492E812CD0581D82F0514675732C24D054AC473EB689D110"));

        MerkleBranch merklePath = new MerkleBranch( 0, subject, path, true);
        Assert.assertTrue(merklePath.verify(Sha256Hash.wrap("9610685BCE24913B2C8562A95A8A4248B216B3ECA4F6D6D0", 24)));
    }

    @Test
    public void verifyWhenFourRegularTransactionsAndPositionThree() {
        Sha256Hash subject = Sha256Hash.wrap("C929570584BEB05F69B974ABAA5E742FC37AC0001F35E0EBB0B8C8E2FEBFB418");
        List<Sha256Hash> path = Arrays.asList(
                Sha256Hash.wrap("F8D22055FDADA935C8977DF3105E014C4E5862EA2FA3E0C37805D00445377932"),
                Sha256Hash.wrap("2328CD579BFAA48229689CAEC41504FC510C81E458FE9097466A728B5390E0DE"),
                Sha256Hash.wrap("0000000000000000000000000000000000000000000000000000000000000000"),
                Sha256Hash.wrap("953855AD90E483B0492E812CD0581D82F0514675732C24D054AC473EB689D110"));

        MerkleBranch merklePath = new MerkleBranch( 3, subject, path, true);
        Assert.assertTrue(merklePath.verify(Sha256Hash.wrap("9D5EC2CEB02657B9B2A74A188FE6C593BABE07B297B99F200DFD065906D35795")));
    }

    @Test
    public void verifyWhenFourRegularTransactionsAndPositionTwo() {
        Sha256Hash subject = Sha256Hash.wrap("C929570584BEB05F69B974ABAA5E742FC37AC0001F35E0EBB0B8C8E2FEBFB418");
        List<Sha256Hash> path = Arrays.asList(
                Sha256Hash.wrap("F8D22055FDADA935C8977DF3105E014C4E5862EA2FA3E0C37805D00445377932"),
                Sha256Hash.wrap("2328CD579BFAA48229689CAEC41504FC510C81E458FE9097466A728B5390E0DE"),
                Sha256Hash.wrap("0000000000000000000000000000000000000000000000000000000000000000"),
                Sha256Hash.wrap("953855AD90E483B0492E812CD0581D82F0514675732C24D054AC473EB689D110"));

        MerkleBranch merklePath = new MerkleBranch( 2, subject, path, true);
        Assert.assertTrue(merklePath.verify(Sha256Hash.wrap("9BBAE0AACC6DD72146C5612B3C625208AFD27EB7F61A3A0FB7D660CA63EF5671")));
    }
}
