// VeriBlock NodeCore CLI
// Copyright 2017-2020 Xenios SEZC
// All rights reserved.
// https://www.veriblock.org
// Distributed under the MIT software license, see the accompanying
// file LICENSE or http://www.opensource.org/licenses/mit-license.php.

package nodecore.cli.contracts;

import java.util.Objects;

public class ProtocolEndpoint {
    private String _password;
    private ProtocolEndpointType _type;
    private EndpointTransportType _transportType;
    private String _address;
    private short _port;

    public ProtocolEndpoint(
            String address,
            ProtocolEndpointType type,
            String password) {
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
        _type = type;
        _address = parts[0];
        _port = Short.parseShort(parts[1]);
        _password = password;

    }

    public ProtocolEndpoint(
            String address,
            short port,
            ProtocolEndpointType type,
            EndpointTransportType transportType) {
        _address = address;
        _port = port;
        _type = type;
        _transportType = transportType;
    }

    @Override
    public boolean equals(Object object) {
        if (!(object instanceof ProtocolEndpoint))
            return false;

        ProtocolEndpoint other = (ProtocolEndpoint) object;
        return other._address.equals(_address) && other._port == _port;
    }

    @Override
    public int hashCode() {
        return Objects.hash(_address, _port);
    }

    @Override
    public String toString() {
        return String.format("%s (%s:%s)", _type, _address, _port);
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

    public ProtocolEndpointType type() {
        return _type;
    }
    public EndpointTransportType transportType() {
        return _transportType;
    }
}
