// VeriBlock NodeCore
// Copyright 2017-2020 Xenios SEZC
// All rights reserved.
// https://www.veriblock.org
// Distributed under the MIT software license, see the accompanying
// file LICENSE or http://www.opensource.org/licenses/mit-license.php.er;

package org.veriblock.extensions.ledger;

import nodecore.api.grpc.VeriBlockMessages;

import java.util.Objects;

/**
 * The LedgerValue class acts only as a leaf node in the ledger tree.
 */
public class LedgerValue {
    /* Funds which are available for spending */
    private long _availableAtomicUnits;

    /* Funds which are frozen in a publication contract, will be returned if contract is unfulfilled */
    private long _frozenAtomicUnits;

    /* The signature index, to prevent transaction replay attacks and force transaction ordering */
    private long _signatureIndex;

    /**
     * Constructs a LedgerValue object with the provided
     *
     * @param availableAtomicUnits The number of atomic VeriBlock coins available for spending
     * @param frozenAtomicUnits    The number of atomic VeriBlock coins frozen in contracts
     * @param signatureIndex       The signature index of the owning address
     */
    public LedgerValue(long availableAtomicUnits, long frozenAtomicUnits, long signatureIndex) {
        _availableAtomicUnits = availableAtomicUnits;
        _frozenAtomicUnits = frozenAtomicUnits;
        _signatureIndex = signatureIndex;
    }


    public LedgerValue(VeriBlockMessages.LedgerValue message) {
        this(message.getAvailableAtomicUnits(), 0L, message.getSignatureIndex());

        if (_availableAtomicUnits < 0) {
            throw new IllegalArgumentException("A LedgerValue cannot be constructed with negative atomic units!");
        }

        if (_signatureIndex < -1) {
            throw new IllegalArgumentException("A LedgerValue cannot be constructed with a signature index below -1!");
        }
    }

    public static LedgerValue getGenesisEntry() {
        return new LedgerValue(0, 0, 0);
    }

    public static LedgerValue getDefaultEntry() {
        return new LedgerValue(0, 0, -1);
    }

    /**
     * Create a human-readable String representation of this object
     */
    public String toString() {
        return String.format(
                "LedgerValue[availableAtomicUnits=%d, frozenAtomicUnits=%d, signatureIndex=%d]",
                _availableAtomicUnits,
                _frozenAtomicUnits,
                _signatureIndex);
    }

    public boolean equals(Object toTest) {
        if (toTest instanceof LedgerValue) {
            LedgerValue other = (LedgerValue) toTest;
            return other._availableAtomicUnits == _availableAtomicUnits
                    && other._frozenAtomicUnits == _frozenAtomicUnits
                    && other._signatureIndex == _signatureIndex;
        }
        return false;
    }

    @Override
    public int hashCode() {
        return Objects.hash(_availableAtomicUnits, _frozenAtomicUnits, _signatureIndex);
    }

    /**
     * A LedgerValue is considered "default" if it has the same contents as a LedgerValue which corresponds
     * to an address which has never been used on the network in a transaction.
     *
     * If at any point isDefault returns true, then the calling code can safely remove the LedgerValue from
     * the ledger tree, as it is as if the address was never used on the network. The only time a LedgerNode
     * would be reverted back to its default/original state is if blockchain fork resolution causes previously-
     * applied modifications to the ledger to be undone.
     *
     * Even an address previously used on the network with zero balance will have a signature index that is not
     * default, as to get back to a zero balance the address would have had to make at least one outgoing transaction.
     * @return Whether the LedgerValue is in its default state
     */
    public boolean isDefault() {
        return getAvailableAtomicUnits() == 0 && getFrozenAtomicUnits() == 0 && getSignatureIndex() == -1;
    }

    public long getAvailableAtomicUnits() {
        return _availableAtomicUnits;
    }

    public long getFrozenAtomicUnits() {
        return _frozenAtomicUnits;
    }

    public long getSignatureIndex() {
        return _signatureIndex;
    }

    public LedgerValue copyOf() {
        return new LedgerValue(this._availableAtomicUnits, this._frozenAtomicUnits, this._signatureIndex);
    }

    public VeriBlockMessages.LedgerValue.Builder getMessageBuilder() {
        VeriBlockMessages.LedgerValue.Builder builder = VeriBlockMessages.LedgerValue.newBuilder();
        builder.setAvailableAtomicUnits(getAvailableAtomicUnits());
        builder.setSignatureIndex(getSignatureIndex());

        return builder;
    }
}
