// VeriBlock NodeCore CLI
// Copyright 2017-2021 Xenios SEZC
// All rights reserved.
// https://www.veriblock.org
// Distributed under the MIT software license, see the accompanying
// file LICENSE or http://www.opensource.org/licenses/mit-license.php.
package nodecore.cli.contracts

class PeerEndpoint(
    val address: String,
    val password: String? = null,
    val port: Short,
    val transportType: EndpointTransportType = EndpointTransportType.HTTP
) {
    override fun toString(): String = "${transportType.toString().toLowerCase()}://$address:$port"

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as PeerEndpoint

        if (address != other.address) return false
        if (port != other.port) return false

        return true
    }

    override fun hashCode(): Int {
        var result = address.hashCode()
        result = 31 * result + port
        return result
    }
}
