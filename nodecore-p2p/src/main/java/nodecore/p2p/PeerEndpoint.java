// VeriBlock NodeCore
// Copyright 2017-2020 Xenios SEZC
// All rights reserved.
// https://www.veriblock.org
// Distributed under the MIT software license, see the accompanying
// file LICENSE or http://www.opensource.org/licenses/mit-license.php.

package nodecore.p2p;

import java.util.Objects;

public class PeerEndpoint {
    private String _address;
    private int _port;

    public PeerEndpoint(String address) {
        String[] parts = address.split(":");
        _address = parts[0];
        _port = Short.parseShort(parts[1]);
    }

    public PeerEndpoint(String address, int port) {
        _address = address;
        _port = port;
    }

    @Override
    public boolean equals(Object object) {
        if (!(object instanceof PeerEndpoint))
            return false;

        PeerEndpoint other = (PeerEndpoint) object;
        return other._address.equals(_address) && other._port == _port;
    }

    @Override
    public int hashCode() {
        return Objects.hash(_address, _port);
    }

    @Override
    public String toString() {
        return String.format("%s:%s", _address, _port);
    }

    public int port() {
        return _port;
    }

    public String address() {
        return _address;
    }

    public String addressKey() {
        return _address + ":" + _port;
    }
}
