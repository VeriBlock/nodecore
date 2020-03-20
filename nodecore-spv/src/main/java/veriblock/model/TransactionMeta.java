// VeriBlock Blockchain Project
// Copyright 2017-2018 VeriBlock, Inc
// Copyright 2018-2020 Xenios SEZC
// All rights reserved.
// https://www.veriblock.org
// Distributed under the MIT software license, see the accompanying
// file LICENSE or http://www.opensource.org/licenses/mit-license.php.
package veriblock.model;

import org.veriblock.sdk.models.Sha256Hash;
import org.veriblock.sdk.models.VBlakeHash;
import veriblock.listeners.TransactionStateChangedListener;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.Executor;

public class TransactionMeta {
    private final List<VBlakeHash> appearsInBlock;
    private int appearsAtChainHeight = -1;
    private int depth;
    private final Set<String> seenByPeers;
    private final CopyOnWriteArrayList<ListenerRegistration<TransactionStateChangedListener>> stateChangedListeners;
    private final Sha256Hash txId;
    private MetaState state = MetaState.UNKNOWN;
    private VBlakeHash appearsInBestChainBlock;

    public TransactionMeta(Sha256Hash txId) {
        this.txId = txId;
        this.appearsInBlock = new ArrayList<>();
        this.stateChangedListeners = new CopyOnWriteArrayList<>();
        this.seenByPeers = new HashSet<>();
    }

    public VBlakeHash getAppearsInBestChainBlock() {
        return appearsInBestChainBlock;
    }

    public void setAppearsInBestChainBlock(VBlakeHash appearsInBestChainBlock) {
        this.appearsInBestChainBlock = appearsInBestChainBlock;
    }


    public List<VBlakeHash> getAppearsInBlock() {
        return appearsInBlock;
    }

    public void addBlockAppearance(VBlakeHash hash) {
        appearsInBlock.add(hash);
    }


    public int getAppearsAtChainHeight() {
        return appearsAtChainHeight;
    }

    public void setAppearsAtChainHeight(int appearsAtChainHeight) {
        this.appearsAtChainHeight = appearsAtChainHeight;
    }

    public int getDepth() {
        return depth;
    }

    public void setDepth(int depth) {
        this.depth = depth;
    }

    public void incrementDepth() {
        this.depth++;
        informListenersDepthChanged();
    }

    public int getBroadcastPeerCount() {
        return seenByPeers.size();
    }

    public boolean recordBroadcast(String peer) {
        boolean added = seenByPeers.add(peer);
        if (!added) {
            return false;
        }

        if (getState() == MetaState.UNKNOWN) {
            setState(MetaState.PENDING);
        }

        return true;
    }

    public void addTransactionStateChangedListener(TransactionStateChangedListener listener, Executor executor) {
        stateChangedListeners.add(new ListenerRegistration<>(listener, executor));
    }

    public void removeTransactionStateChangedListener(TransactionStateChangedListener listener) {
        ListenerRegistration.removeFromList(listener, stateChangedListeners);
    }

    private void informListenersStateChanged() {
        TransactionMeta self = this;
        for (final ListenerRegistration<TransactionStateChangedListener> registration : stateChangedListeners) {
            registration.executor.execute(() -> registration.listener.onTransactionStateChanged(self,
                    TransactionStateChangedListener.ChangeReason.STATE));
        }
    }

    private void informListenersDepthChanged() {
        TransactionMeta self = this;
        for (final ListenerRegistration<TransactionStateChangedListener> registration : stateChangedListeners) {
            registration.executor.execute(() -> registration.listener.onTransactionStateChanged(self,
                    TransactionStateChangedListener.ChangeReason.DEPTH));
        }
    }

    public Sha256Hash getTxId() {
        return txId;
    }

    public MetaState getState() {
        return state;
    }

    public void setState(MetaState state) {
        if (this.state == state) return;

        this.state = state;
        if (state == MetaState.PENDING) {
            depth = 0;
            appearsAtChainHeight = -1;
        }
        informListenersStateChanged();
    }

    public enum MetaState {
        PENDING(1),
        CONFIRMED(2),
        DEAD(3),
        UNKNOWN(0);

        private int value;

        MetaState(int value) {
            this.value = value;
        }

        public int getValue() {
            return value;
        }

        public static MetaState forNumber(int value) {
            switch (value) {
                case 0:
                    return UNKNOWN;
                case 1:
                    return PENDING;
                case 2:
                    return CONFIRMED;
                case 3:
                    return DEAD;
                default:
                    return null;
            }
        }
    }

}
