// VeriBlock Blockchain Project
// Copyright 2017-2018 VeriBlock, Inc
// Copyright 2018-2020 Xenios SEZC
// All rights reserved.
// https://www.veriblock.org
// Distributed under the MIT software license, see the accompanying
// file LICENSE or http://www.opensource.org/licenses/mit-license.php.

package veriblock.net;

import nodecore.api.grpc.VeriBlockMessages;
import veriblock.model.DownloadStatusResponse;
import veriblock.model.LedgerContext;
import veriblock.model.Transaction;

import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;

/**
 * Service for working with peers.
 */
public interface PeerTable {

    void advertise(Transaction signedTransaction);

    Future<VeriBlockMessages.Event> advertiseWithReply(VeriBlockMessages.Event event);

    void start();

    void shutdown();

    Long getSignatureIndex(String address);

    Integer getAvailablePeers();

    Integer getBestBlockHeight();

    Map<String, LedgerContext> getAddressesState();

    LedgerContext getAddressState(String address);

    DownloadStatusResponse getDownloadStatus() throws ExecutionException, InterruptedException;
}
