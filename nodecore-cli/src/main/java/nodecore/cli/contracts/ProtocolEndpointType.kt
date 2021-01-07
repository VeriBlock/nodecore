// VeriBlock NodeCore CLI
// Copyright 2017-2020 Xenios SEZC
// All rights reserved.
// https://www.veriblock.org
// Distributed under the MIT software license, see the accompanying
// file LICENSE or http://www.opensource.org/licenses/mit-license.php.
package nodecore.cli.contracts

enum class ProtocolEndpointType {
    NONE,
    PEER,
    RPC;

    override fun toString(): String {
        return when (name) {
            "PEER" -> "p2p"
            "NONE" -> "(no connection)"
            else -> "rpc"
        }
    }
}
