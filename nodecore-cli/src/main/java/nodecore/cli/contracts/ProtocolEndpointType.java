// VeriBlock NodeCore CLI
// Copyright 2017-2020 Xenios SEZC
// All rights reserved.
// https://www.veriblock.org
// Distributed under the MIT software license, see the accompanying
// file LICENSE or http://www.opensource.org/licenses/mit-license.php.

package nodecore.cli.contracts;

public enum ProtocolEndpointType {
    NONE,
    PEER,
    RPC;

    public String toString() {
        switch (name()) {
            case "PEER":
                return "p2p";
            case "NONE":
                return "(no connection)";
            default:
                return "rpc";
        }
    }
}
