// VeriBlock NodeCore
// Copyright 2017-2021 Xenios SEZC
// All rights reserved.
// https://www.veriblock.org
// Distributed under the MIT software license, see the accompanying
// file LICENSE or http://www.opensource.org/licenses/mit-license.php.
package nodecore.p2p

import java.util.EnumSet
import nodecore.p2p.PeerCapabilities
import org.apache.commons.lang3.EnumUtils

class PeerCapabilities(
    private val capabilities: Set<Capabilities>
) {
    enum class Capabilities {
        Transaction, Block, Query, Sync, NetworkInfo, BatchSync, Advertise, AdvertiseTx, SpvRequests, VtbRequests
    }
    
    private constructor(bitVector: Long) : this(EnumUtils.processBitVector(Capabilities::class.java, bitVector))
    
    fun toBitVector(): Long {
        return EnumUtils.generateBitVector(Capabilities::class.java, capabilities)
    }
    
    fun hasCapability(capability: Capabilities): Boolean {
        return capabilities.contains(capability)
    }
    
    companion object {
        private val INITIAL_CAPABILITIES = EnumSet.of(
            Capabilities.Transaction,
            Capabilities.Block,
            Capabilities.Query,
            Capabilities.Sync,
            Capabilities.NetworkInfo,
            Capabilities.BatchSync
        )
        private val SPV_CAPABILITIES = EnumSet.of(
            Capabilities.Transaction,
            Capabilities.Query,
            Capabilities.NetworkInfo,
        )

        private val ALL = EnumSet.allOf(Capabilities::class.java)

        @JvmStatic
        fun allCapabilities(except: Set<Capabilities> = emptySet()): PeerCapabilities {
            return PeerCapabilities(ALL - except)
        }

        @JvmStatic
        fun defaultCapabilities(): PeerCapabilities {
            return PeerCapabilities(INITIAL_CAPABILITIES)
        }

        @JvmStatic
        fun spvCapabilities(): PeerCapabilities {
            return PeerCapabilities(SPV_CAPABILITIES)
        }

        @JvmStatic
        fun parse(bitVector: Long): PeerCapabilities {
            return if (bitVector <= 0) {
                defaultCapabilities()
            } else {
                PeerCapabilities(bitVector)
            }
        }
    }
}
