// VeriBlock NodeCore
// Copyright 2017-2021 Xenios SEZC
// All rights reserved.
// https://www.veriblock.org
// Distributed under the MIT software license, see the accompanying
// file LICENSE or http://www.opensource.org/licenses/mit-license.php.
package org.veriblock.spv

import nodecore.p2p.Peer
import org.veriblock.core.utilities.createLogger
import java.util.concurrent.ConcurrentHashMap

private val logger = createLogger {}

object SpvState {
    private val networkHeight = ConcurrentHashMap<String, Int>()

    var downloadPeer: Peer? = null

    var localBlockchainHeight = 0
        set(value) {
            if (value > field) {
                logger.debug("Setting local blockchain height to {}", value)
                field = value
            }
        }
    
    fun getNetworkHeight(): Int {
        return networkHeight.values.maxOrNull() ?: 0
    }
    
    fun putNetworkHeight(peer: String, height: Int) {
        networkHeight[peer] = height
    }

    fun getAllPeerHeights(): Map<String, Int> = networkHeight

    fun getPeerHeight(peer: Peer) = networkHeight[peer.addressKey] ?: 0
}
