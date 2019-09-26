// VeriBlock PoP Miner
// Copyright 2017-2019 Xenios SEZC
// All rights reserved.
// https://www.veriblock.org
// Distributed under the MIT software license, see the accompanying
// file LICENSE or http://www.opensource.org/licenses/mit-license.php.

package nodecore.miners.pop.contracts;

import org.apache.commons.lang3.tuple.Pair;
import org.bitcoinj.core.Coin;
import org.bitcoinj.core.StoredBlock;

import java.math.BigDecimal;
import java.util.List;

public interface PoPMiner extends Runnable {
    void shutdown() throws InterruptedException;

    List<OperationSummary> listOperations();

    PreservedPoPMiningOperationState getOperationState(String id);

    boolean isReady();

    MineResult mine(Integer blockNumber);

    Result resubmit(String id);

    String getMinerAddress();

    Coin getBitcoinBalance();
    
    StoredBlock getLastBitcoinBlock();

    String getBitcoinReceiveAddress();

    Pair<Integer, Long> showRecentBitcoinFees();

    List<String> getWalletSeed();

    void agreeToWalletSeedRequirement();

    boolean importWallet(List<String> seedWords, Long creationDate);

    Result sendBitcoinToAddress(String address, BigDecimal amount);

    Result resetBitcoinWallet();

    Result exportBitcoinPrivateKeys();
}
