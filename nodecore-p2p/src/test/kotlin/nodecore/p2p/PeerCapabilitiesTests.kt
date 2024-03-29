// VeriBlock NodeCore
// Copyright 2017-2021 Xenios SEZC
// All rights reserved.
// https://www.veriblock.org
// Distributed under the MIT software license, see the accompanying
// file LICENSE or http://www.opensource.org/licenses/mit-license.php.
package nodecore.tests.p2p

import io.kotest.matchers.shouldBe
import nodecore.p2p.PeerCapabilities
import org.junit.Test

class PeerCapabilitiesTests {
    @Test
    fun hasCapabilityWithDefaults() {
        val peerCapabilities = PeerCapabilities.defaultCapabilities()
        peerCapabilities.hasCapability(PeerCapabilities.Capability.Transaction) shouldBe true
        peerCapabilities.hasCapability(PeerCapabilities.Capability.Block) shouldBe true
        peerCapabilities.hasCapability(PeerCapabilities.Capability.Query) shouldBe true
        peerCapabilities.hasCapability(PeerCapabilities.Capability.Sync) shouldBe true
        peerCapabilities.hasCapability(PeerCapabilities.Capability.NetworkInfo) shouldBe true
        peerCapabilities.hasCapability(PeerCapabilities.Capability.BatchSync) shouldBe true
        peerCapabilities.hasCapability(PeerCapabilities.Capability.Advertise) shouldBe false
        peerCapabilities.hasCapability(PeerCapabilities.Capability.AdvertiseTx) shouldBe false
    }

    @Test
    fun hasCapabilityWithAll() {
        val peerCapabilities = PeerCapabilities.allCapabilities()
        peerCapabilities.hasCapability(PeerCapabilities.Capability.Transaction) shouldBe true
        peerCapabilities.hasCapability(PeerCapabilities.Capability.Block) shouldBe true
        peerCapabilities.hasCapability(PeerCapabilities.Capability.Query) shouldBe true
        peerCapabilities.hasCapability(PeerCapabilities.Capability.Sync) shouldBe true
        peerCapabilities.hasCapability(PeerCapabilities.Capability.NetworkInfo) shouldBe true
        peerCapabilities.hasCapability(PeerCapabilities.Capability.BatchSync) shouldBe true
        peerCapabilities.hasCapability(PeerCapabilities.Capability.Advertise) shouldBe true
        peerCapabilities.hasCapability(PeerCapabilities.Capability.AdvertiseTx) shouldBe true
    }

    @Test
    fun toBitVectorWithDefault() {
        val peerCapabilities = PeerCapabilities.defaultCapabilities()
        val bitVector = peerCapabilities.toBitVector()
        bitVector shouldBe 63L
    }

    @Test
    fun toBitVectorWithAll() {
        val peerCapabilities = PeerCapabilities.allCapabilities()
        val bitVector = peerCapabilities.toBitVector()
        bitVector shouldBe 1023L
    }

    @Test
    fun parseTransaction() {
        val peerCapabilities = PeerCapabilities.parse(1L)
        peerCapabilities.hasCapability(PeerCapabilities.Capability.Transaction) shouldBe true
        peerCapabilities.hasCapability(PeerCapabilities.Capability.Block) shouldBe false
        peerCapabilities.hasCapability(PeerCapabilities.Capability.Query) shouldBe false
        peerCapabilities.hasCapability(PeerCapabilities.Capability.Sync) shouldBe false
        peerCapabilities.hasCapability(PeerCapabilities.Capability.NetworkInfo) shouldBe false
        peerCapabilities.hasCapability(PeerCapabilities.Capability.BatchSync) shouldBe false
        peerCapabilities.hasCapability(PeerCapabilities.Capability.Advertise) shouldBe false
        peerCapabilities.hasCapability(PeerCapabilities.Capability.AdvertiseTx) shouldBe false
    }

    @Test
    fun parseBlock() {
        val peerCapabilities = PeerCapabilities.parse(2L)
        peerCapabilities.hasCapability(PeerCapabilities.Capability.Transaction) shouldBe false
        peerCapabilities.hasCapability(PeerCapabilities.Capability.Block) shouldBe true
        peerCapabilities.hasCapability(PeerCapabilities.Capability.Query) shouldBe false
        peerCapabilities.hasCapability(PeerCapabilities.Capability.Sync) shouldBe false
        peerCapabilities.hasCapability(PeerCapabilities.Capability.NetworkInfo) shouldBe false
        peerCapabilities.hasCapability(PeerCapabilities.Capability.BatchSync) shouldBe false
        peerCapabilities.hasCapability(PeerCapabilities.Capability.Advertise) shouldBe false
        peerCapabilities.hasCapability(PeerCapabilities.Capability.AdvertiseTx) shouldBe false
    }

    @Test
    fun parseQuery() {
        val peerCapabilities = PeerCapabilities.parse(4L)
        peerCapabilities.hasCapability(PeerCapabilities.Capability.Transaction) shouldBe false
        peerCapabilities.hasCapability(PeerCapabilities.Capability.Block) shouldBe false
        peerCapabilities.hasCapability(PeerCapabilities.Capability.Query) shouldBe true
        peerCapabilities.hasCapability(PeerCapabilities.Capability.Sync) shouldBe false
        peerCapabilities.hasCapability(PeerCapabilities.Capability.NetworkInfo) shouldBe false
        peerCapabilities.hasCapability(PeerCapabilities.Capability.BatchSync) shouldBe false
        peerCapabilities.hasCapability(PeerCapabilities.Capability.Advertise) shouldBe false
        peerCapabilities.hasCapability(PeerCapabilities.Capability.AdvertiseTx) shouldBe false
    }

    @Test
    fun parseSync() {
        val peerCapabilities = PeerCapabilities.parse(8L)
        peerCapabilities.hasCapability(PeerCapabilities.Capability.Transaction) shouldBe false
        peerCapabilities.hasCapability(PeerCapabilities.Capability.Block) shouldBe false
        peerCapabilities.hasCapability(PeerCapabilities.Capability.Query) shouldBe false
        peerCapabilities.hasCapability(PeerCapabilities.Capability.Sync) shouldBe true
        peerCapabilities.hasCapability(PeerCapabilities.Capability.NetworkInfo) shouldBe false
        peerCapabilities.hasCapability(PeerCapabilities.Capability.BatchSync) shouldBe false
        peerCapabilities.hasCapability(PeerCapabilities.Capability.Advertise) shouldBe false
        peerCapabilities.hasCapability(PeerCapabilities.Capability.AdvertiseTx) shouldBe false
    }

    @Test
    fun parseNetworkInfo() {
        val peerCapabilities = PeerCapabilities.parse(16L)
        peerCapabilities.hasCapability(PeerCapabilities.Capability.Transaction) shouldBe false
        peerCapabilities.hasCapability(PeerCapabilities.Capability.Block) shouldBe false
        peerCapabilities.hasCapability(PeerCapabilities.Capability.Query) shouldBe false
        peerCapabilities.hasCapability(PeerCapabilities.Capability.Sync) shouldBe false
        peerCapabilities.hasCapability(PeerCapabilities.Capability.NetworkInfo) shouldBe true
        peerCapabilities.hasCapability(PeerCapabilities.Capability.BatchSync) shouldBe false
        peerCapabilities.hasCapability(PeerCapabilities.Capability.Advertise) shouldBe false
        peerCapabilities.hasCapability(PeerCapabilities.Capability.AdvertiseTx) shouldBe false
    }

    @Test
    fun parseBatchSync() {
        val peerCapabilities = PeerCapabilities.parse(32L)
        peerCapabilities.hasCapability(PeerCapabilities.Capability.Transaction) shouldBe false
        peerCapabilities.hasCapability(PeerCapabilities.Capability.Block) shouldBe false
        peerCapabilities.hasCapability(PeerCapabilities.Capability.Query) shouldBe false
        peerCapabilities.hasCapability(PeerCapabilities.Capability.Sync) shouldBe false
        peerCapabilities.hasCapability(PeerCapabilities.Capability.NetworkInfo) shouldBe false
        peerCapabilities.hasCapability(PeerCapabilities.Capability.BatchSync) shouldBe true
        peerCapabilities.hasCapability(PeerCapabilities.Capability.Advertise) shouldBe false
        peerCapabilities.hasCapability(PeerCapabilities.Capability.AdvertiseTx) shouldBe false
    }

    @Test
    fun parseAdvertise() {
        val peerCapabilities = PeerCapabilities.parse(64L)
        peerCapabilities.hasCapability(PeerCapabilities.Capability.Transaction) shouldBe false
        peerCapabilities.hasCapability(PeerCapabilities.Capability.Block) shouldBe false
        peerCapabilities.hasCapability(PeerCapabilities.Capability.Query) shouldBe false
        peerCapabilities.hasCapability(PeerCapabilities.Capability.Sync) shouldBe false
        peerCapabilities.hasCapability(PeerCapabilities.Capability.NetworkInfo) shouldBe false
        peerCapabilities.hasCapability(PeerCapabilities.Capability.BatchSync) shouldBe false
        peerCapabilities.hasCapability(PeerCapabilities.Capability.Advertise) shouldBe true
        peerCapabilities.hasCapability(PeerCapabilities.Capability.AdvertiseTx) shouldBe false
    }

    @Test
    fun parseAdvertiseTx() {
        val peerCapabilities = PeerCapabilities.parse(128L)
        peerCapabilities.hasCapability(PeerCapabilities.Capability.Transaction) shouldBe false
        peerCapabilities.hasCapability(PeerCapabilities.Capability.Block) shouldBe false
        peerCapabilities.hasCapability(PeerCapabilities.Capability.Query) shouldBe false
        peerCapabilities.hasCapability(PeerCapabilities.Capability.Sync) shouldBe false
        peerCapabilities.hasCapability(PeerCapabilities.Capability.NetworkInfo) shouldBe false
        peerCapabilities.hasCapability(PeerCapabilities.Capability.BatchSync) shouldBe false
        peerCapabilities.hasCapability(PeerCapabilities.Capability.Advertise) shouldBe false
        peerCapabilities.hasCapability(PeerCapabilities.Capability.AdvertiseTx) shouldBe true
    }
}
