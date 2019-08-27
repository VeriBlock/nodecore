// VeriBlock NodeCore CLI
// Copyright 2017-2019 Xenios SEZC
// All rights reserved.
// https://www.veriblock.org
// Distributed under the MIT software license, see the accompanying
// file LICENSE or http://www.opensource.org/licenses/mit-license.php.

package nodecore.cli.commands.serialization;

import nodecore.api.grpc.utilities.ByteStringUtility;

public class TransactionReferenceInfo {
    public TransactionReferenceInfo(final nodecore.api.grpc.TransactionInfo transactionInfo) {
        blockNum = transactionInfo.getBlockNumber();
        confirmations = transactionInfo.getConfirmations();
        transaction = new TransactionInfo(transactionInfo.getTransaction());
        timestamp = transactionInfo.getTimestamp();
        endorsedBlockHash = ByteStringUtility.byteStringToHex(transactionInfo.getEndorsedBlockHash());
        bitcoinBlockHash = ByteStringUtility.byteStringToHex(transactionInfo.getBitcoinBlockHash());
        bitcoinTxID = ByteStringUtility.byteStringToHex(transactionInfo.getBitcoinTxId());
    }

    public int timestamp;

    public int blockNum;

    public int confirmations;

    public TransactionInfo transaction;

    public String endorsedBlockHash;

    public String bitcoinBlockHash;

    public String bitcoinTxID;
}
