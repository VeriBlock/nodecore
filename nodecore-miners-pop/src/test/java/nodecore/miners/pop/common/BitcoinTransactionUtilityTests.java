// VeriBlock PoP Miner
// Copyright 2017-2019 Xenios SEZC
// All rights reserved.
// https://www.veriblock.org
// Distributed under the MIT software license, see the accompanying
// file LICENSE or http://www.opensource.org/licenses/mit-license.php.

package nodecore.miners.pop.common;

import org.junit.Assert;
import org.junit.Test;

public class BitcoinTransactionUtilityTests {
    @Test
    public void getTransactionIdWithSegwit() {
        String signedTransaction =
                "0200000000010178bcedb9c5480332dd958833fa84c6576631cc345744b7cb2eebaa37e948f5e80100000017160014cf5ae2aea646776616ce8077ac2e1a8c4d8aac9dffffffff020000000000000000536a4c5040bd8cef8beba0130c1b25228e90cb68859f1e70f8264b81d51d899d1f1d6d13d53d6dbefda79d59297e8d3f0e4a48a129af29bad014a3802680e6fd9cac68cbf3ec1411000b003908138d51524f015300143e030000000017a914bd15ed9937f7f116c402ce9982151bb927ec25598702473044022038902fd9963d812b15577e96e07fb1c344fcd5d46d8d1d9f8836a757bc680f97022045f33d68cdc9e61436f6c2d8acf6ec58d3d371f3f76e0b6133bfa65e2db2afdc0121035d17524f37562deabb1b611cceab33d89e792fcc1a4513fb8de4db839517083d00000000";
        byte[] transactionWithWitnessDataRemoved = BitcoinTransactionUtility.parseTxIDRelevantBits(Utility.hexToBytes(signedTransaction));
        String txId = BitcoinTransactionUtility.getTransactionId(transactionWithWitnessDataRemoved);
        Assert.assertEquals("7c5ff70e97356747aa03995d38ad39f952abc7aa979a86b07c97501bf6bf10cb", txId.toLowerCase());
    }

    @Test
    public void getTransactionId() {
        String signedTransaction =
                "02000000000101a2a6df07df27efb74661dea49e45be43d2109024e0bd495967fff35964906e5f0000000017160014796430b1cbc42e417c7b453f4dba76604a8a500ffdffffff014b3e76e80000000017a914501c479d143b6f9933c166b1ea243e9dd30c499b8702483045022100b1d8238022db661f874226357647bc930025b77df33ce14779c29984780fdff302205e2c32e4ace09c8f2ef610dee3f561e3819f3b6c5e4dea8d603cf56e510bdc72012103dc6a885f3ffff9f7bc76d5b620727fb08cb15e52bca8dc0e4bbf9cb14867f88121ae1300";
        byte[] transactionWithWitnessDataRemoved = BitcoinTransactionUtility.parseTxIDRelevantBits(Utility.hexToBytes(signedTransaction));
        String txId = BitcoinTransactionUtility.getTransactionId(transactionWithWitnessDataRemoved);
        Assert.assertEquals("4d3aff9d22ecbbe00070da9b8ad6d91b055470cf0452141fb887be294f956ea4", txId.toLowerCase());
    }
}
