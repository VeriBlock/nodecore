// VeriBlock NodeCore
// Copyright 2017-2020 Xenios SEZC
// All rights reserved.
// https://www.veriblock.org
// Distributed under the MIT software license, see the accompanying
// file LICENSE or http://www.opensource.org/licenses/mit-license.php.

package nodecore.cli;

import nodecore.cli.contracts.ProtocolEndpoint;

public class ProtocolEndpointContainer {
    private ProtocolEndpoint _protocolEndpoint = null;

    public void setProtocolEndpoint(ProtocolEndpoint protocolEndpoint) {
        this._protocolEndpoint = protocolEndpoint;
    }

    public ProtocolEndpoint getProtocolEndpoint() {
        return _protocolEndpoint;
    }
}
