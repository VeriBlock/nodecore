// VeriBlock NodeCore
// Copyright 2017-2021 Xenios SEZC
// All rights reserved.
// https://www.veriblock.org
// Distributed under the MIT software license, see the accompanying
// file LICENSE or http://www.opensource.org/licenses/mit-license.php.
package nodecore.p2p

import java.util.EnumSet
import org.apache.commons.lang3.EnumUtils

class PeerCapabilities(
    private val capabilities: Set<Capability>
) {
    enum class Capability {
        Transaction, Block, Query, Sync, NetworkInfo, BatchSync, Advertise, AdvertiseTx, SpvRequests, VtbRequests
    }
    
    private constructor(bitVector: Long) : this(EnumUtils.processBitVector(Capability::class.java, bitVector))
    
    fun toBitVector(): Long {
        return EnumUtils.generateBitVector(Capability::class.java, capabilities)
    }

    fun hasCapability(capability: Capability): Boolean {
        return capabilities.contains(capability)
    }

    fun hasCapabilities(capabilities: PeerCapabilities): Boolean {
        return this.capabilities.containsAll(capabilities.capabilities)
    }

    infix fun and(capabilities: Set<Capability>): PeerCapabilities =
        PeerCapabilities(this.capabilities + capabilities)

    infix fun except(capabilities: Set<Capability>): PeerCapabilities =
        PeerCapabilities(this.capabilities - capabilities)
    
    companion object {
        private val INITIAL_CAPABILITIES = EnumSet.of(
            Capability.Transaction,
            Capability.Block,
            Capability.Query,
            Capability.Sync,
            Capability.NetworkInfo,
            Capability.BatchSync
        )
        private val SPV_CAPABILITIES = EnumSet.of(
            Capability.Transaction,
            Capability.Query,
            Capability.NetworkInfo,
        )

        private val ALL = EnumSet.allOf(Capability::class.java)

        @JvmStatic
        fun allCapabilities(): PeerCapabilities {
            return PeerCapabilities(ALL)
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
