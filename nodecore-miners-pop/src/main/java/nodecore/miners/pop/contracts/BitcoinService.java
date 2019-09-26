// VeriBlock PoP Miner
// Copyright 2017-2019 Xenios SEZC
// All rights reserved.
// https://www.veriblock.org
// Distributed under the MIT software license, see the accompanying
// file LICENSE or http://www.opensource.org/licenses/mit-license.php.

package nodecore.miners.pop.contracts;

import com.google.common.util.concurrent.ListenableFuture;
import nodecore.miners.pop.common.BitcoinMerkleTree;
import org.apache.commons.lang3.tuple.Pair;
import org.bitcoinj.core.*;
import org.bitcoinj.script.Script;
import org.bitcoinj.wallet.Wallet;

import java.util.Collection;
import java.util.List;
import java.util.concurrent.ExecutionException;

public interface BitcoinService {
    void initialize();

    boolean serviceReady();

    String currentReceiveAddress();

    Coin getBalance();

    boolean blockchainDownloaded();

    Script generatePoPScript(byte[] opReturnData);

    ListenableFuture<Transaction> createPoPTransaction(Script opReturnScript) throws ApplicationExceptions.SendTransactionException;

    Block getBlock(Sha256Hash hash);
    
    StoredBlock getLastBlock();

    Block getBestBlock(Collection<Sha256Hash> hashes);

    ListenableFuture<FilteredBlock> getFilteredBlockFuture(Sha256Hash hash);

    PartialMerkleTree getPartialMerkleTree(Sha256Hash hash);

    Block makeBlock(byte[] raw);

    Collection<Block> makeBlocks(Collection<byte[]> raw);

    Transaction makeTransaction(byte[] raw);

    Transaction sendCoins(String address, Coin amount) throws ApplicationExceptions.SendTransactionException;

    void resetWallet();

    Pair<Integer, Long> calculateFeesFromLatestBlock();

    List<String> getMnemonicSeed();

    boolean importWallet(String seedWords, Long creationTime);

    List<String> exportPrivateKeys();

    void shutdown();
}
