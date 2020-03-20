// VeriBlock NodeCore
// Copyright 2017-2020 Xenios SEZC
// All rights reserved.
// https://www.veriblock.org
// Distributed under the MIT software license, see the accompanying
// file LICENSE or http://www.opensource.org/licenses/mit-license.php.

package nodecore.p2p;

import org.apache.commons.lang3.EnumUtils;

import java.util.EnumSet;

public class PeerCapabilities {
    public enum Capabilities {
        Transaction,
        Block,
        Query,
        Sync,
        NetworkInfo,
        BatchSync,
        Advertise,
        AdvertiseTx
    }

    private static final EnumSet<Capabilities> DEFAULT_CAPABILITIES = EnumSet.of(
            Capabilities.Transaction,
            Capabilities.Block,
            Capabilities.Query,
            Capabilities.Sync,
            Capabilities.NetworkInfo,
            Capabilities.BatchSync);

    private static final EnumSet<Capabilities> ALL = EnumSet.allOf(Capabilities.class);

    private final EnumSet<Capabilities> capabilities;

    private PeerCapabilities(EnumSet<Capabilities> capabilities) {
        this.capabilities = capabilities;
    }

    private PeerCapabilities(long bitVector) {
        capabilities = EnumUtils.processBitVector(Capabilities.class, bitVector);
    }

    public long toBitVector() {
        return EnumUtils.generateBitVector(Capabilities.class, this.capabilities);
    }

    public boolean hasCapability(Capabilities capability) {
        return capabilities.contains(capability);
    }

    public static PeerCapabilities allCapabilities() {
        return new PeerCapabilities(ALL);
    }

    public static PeerCapabilities defaultCapabilities() {
        return new PeerCapabilities(DEFAULT_CAPABILITIES);
    }

    public static PeerCapabilities parse(long bitVector) {
        if (bitVector <= 0) {
            return defaultCapabilities();
        }

        return new PeerCapabilities(bitVector);
    }
}
