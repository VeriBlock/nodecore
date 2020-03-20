// VeriBlock NodeCore
// Copyright 2017-2020 Xenios SEZC
// All rights reserved.
// https://www.veriblock.org
// Distributed under the MIT software license, see the accompanying
// file LICENSE or http://www.opensource.org/licenses/mit-license.php.

package nodecore.p2p;


import org.veriblock.core.utilities.Utility;

public class Ban {
    public static final long BAN_DURATION = 60 * 60 * 4; // 4 hours

    public enum Type {
        Temporary,
        Permanent
    }

    private final Type type;
    public Type getType() {
        return type;
    }

    private final String address;
    public String getAddress() {
        return address;
    }

    private final long expiration;
    public long getExpiration() {return expiration; }

    private Ban(Type type, String address, long expiration) {
        this.type = type;
        this.address = address;
        this.expiration = expiration;
    }

    public boolean isExpired() {
        if (getType() == Type.Permanent) return false;

        return Utility.getCurrentTimeSeconds() > getExpiration();
    }

    public static Ban newTemporary(String address) {
        return new Ban(Type.Temporary, address, (Utility.getCurrentTimeSeconds() + BAN_DURATION));
    }

    public static Ban newPermanent(String address) {
        return new Ban(Type.Permanent, address, -1);
    }
}
