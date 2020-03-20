// VeriBlock NodeCore CLI
// Copyright 2017-2020 Xenios SEZC
// All rights reserved.
// https://www.veriblock.org
// Distributed under the MIT software license, see the accompanying
// file LICENSE or http://www.opensource.org/licenses/mit-license.php.

package nodecore.cli.contracts;

import com.google.gson.annotations.SerializedName;

import java.util.Objects;

public class PeerEndpoint {
    @SerializedName("address")
    private String _address;

    @SerializedName("password")
    private String _password;

    @SerializedName("port")
    private short _port;

    @SerializedName("transportType")
    private EndpointTransportType _transportType;

    public PeerEndpoint(String address) {
        if(address.toLowerCase().startsWith("http://")) {
            _transportType = EndpointTransportType.HTTP;
            address = address.toLowerCase().replace("http://", "");
        }
        else if(address.toLowerCase().startsWith("https://")) {
            _transportType = EndpointTransportType.HTTPS;
            address = address.toLowerCase().replace("https://", "");
        }
        else
            _transportType = EndpointTransportType.HTTP;

        String[] parts = address.split(":");
        _address = parts[0];
        _port = Short.parseShort(parts[1]);
    }

    public PeerEndpoint(String address, short port, EndpointTransportType transportType) {
        if(transportType == null)
            transportType = EndpointTransportType.HTTP;

        _address = address;
        _port = port;
        _transportType = transportType;
    }

    public PeerEndpoint(String address, short port, EndpointTransportType transportType, String password) {
        if(transportType == null)
            transportType = EndpointTransportType.HTTP;

        _address = address;
        _port = port;
        _transportType = transportType;
        _password = password;
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
        return String.format("%s://%s:%s", _transportType.toString().toLowerCase(), _address, _port);
    }

    public short port() {
        return _port;
    }

    public String address() {
        return _address;
    }
    public String password() {
        return _password;
    }

    public EndpointTransportType transportType() { return _transportType; }
}
