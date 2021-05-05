// VeriBlock NodeCore
// Copyright 2017-2021 Xenios SEZC
// All rights reserved.
// https://www.veriblock.org
// Distributed under the MIT software license, see the accompanying
// file LICENSE or http://www.opensource.org/licenses/mit-license.php.

package nodecore.tests.p2p

import io.kotest.matchers.shouldBe
import io.ktor.util.network.port
import io.mockk.every
import io.mockk.mockk
import nodecore.p2p.DnsResolver
import nodecore.p2p.P2pConfiguration
import nodecore.p2p.PeerTableBootstrapper
import nodecore.p2p.address
import org.junit.Test
import org.veriblock.core.params.defaultAlphaNetParameters
import org.veriblock.core.params.defaultMainNetParameters
import org.veriblock.core.params.defaultTestNetParameters

class PeerTableBootstrapperTests {
    @Test
    fun getNext_whenSingleAlphaBootstrapPeer() {
        val config: P2pConfiguration = mockk()
        every { config.networkParameters } returns defaultAlphaNetParameters
        every { config.bootstrapPeers } returns listOf("1.1.1.1")
        every { config.bootstrappingDnsSeeds } returns emptyList()
        val dnsResolver: DnsResolver = mockk()
        val bootstrapper = PeerTableBootstrapper(config, dnsResolver)
        val next = bootstrapper.getNext()!!
        next.address shouldBe "1.1.1.1"
        next.port.toLong() shouldBe defaultAlphaNetParameters.p2pPort.toLong()
    }

    @Test
    fun getNext_whenSingleTestNetBootstrapPeer() {
        val config: P2pConfiguration = mockk()
        every { config.networkParameters } returns defaultTestNetParameters
        every { config.bootstrapPeers } returns listOf("1.1.1.1")
        every { config.bootstrappingDnsSeeds } returns emptyList()
        val dnsResolver: DnsResolver = mockk()
        val bootstrapper = PeerTableBootstrapper(config, dnsResolver)
        val next = bootstrapper.getNext()!!
        next.address shouldBe "1.1.1.1"
        next.port.toLong() shouldBe defaultTestNetParameters.p2pPort.toLong()
    }

    @Test
    fun getNext_whenSingleMainNetBootstrapPeer() {
        val config: P2pConfiguration = mockk()
        every { config.networkParameters } returns defaultMainNetParameters
        every { config.bootstrapPeers } returns listOf("1.1.1.1")
        every { config.bootstrappingDnsSeeds } returns emptyList()
        val dnsResolver: DnsResolver = mockk()
        val bootstrapper = PeerTableBootstrapper(config, dnsResolver)
        val next = bootstrapper.getNext()!!
        next.address shouldBe "1.1.1.1"
        next.port.toLong() shouldBe defaultMainNetParameters.p2pPort.toLong()
    }

    @Test
    fun getNext_whenDnsSeeded() {
        val config: P2pConfiguration = mockk()
        every { config.networkParameters } returns defaultAlphaNetParameters
        every { config.bootstrapPeers } returns emptyList()
        every { config.bootstrappingDnsSeeds } returns listOf("seed.veriblock.org")
        val dnsResolver: DnsResolver = mockk()
        every { dnsResolver.query(any()) } returns listOf("1.1.1.1")
        val bootstrapper = PeerTableBootstrapper(config, dnsResolver)
        val next = bootstrapper.getNext()!!
        next.address shouldBe "1.1.1.1"
        next.port.toLong() shouldBe defaultAlphaNetParameters.p2pPort.toLong()
    }

    @Test
    fun getNext_whenNoSeedsConfigured() {
        val config: P2pConfiguration = mockk()
        every { config.networkParameters } returns defaultAlphaNetParameters
        every { config.bootstrapPeers } returns emptyList()
        every { config.bootstrappingDnsSeeds } returns emptyList()
        val dnsResolver: DnsResolver = mockk()
        val bootstrapper = PeerTableBootstrapper(config, dnsResolver)
        val next = bootstrapper.getNext()
        next shouldBe null
    }

    @Test
    fun getNext_whenCountAndNoSeedConfigured() {
        val config: P2pConfiguration = mockk()
        every { config.networkParameters } returns defaultAlphaNetParameters
        every { config.bootstrapPeers } returns emptyList()
        every { config.bootstrappingDnsSeeds } returns emptyList()
        val dnsResolver: DnsResolver = mockk()
        val bootstrapper = PeerTableBootstrapper(config, dnsResolver)
        val next = bootstrapper.getNext(1)
        next.size.toLong() shouldBe 0
    }

    @Test
    fun getNext_whenRequestingLessThanConfigured() {
        val config: P2pConfiguration = mockk()
        every { config.networkParameters } returns defaultAlphaNetParameters
        every { config.bootstrapPeers } returns listOf("1.1.1.1")
        every { config.bootstrappingDnsSeeds } returns emptyList()
        val dnsResolver: DnsResolver = mockk()
        val bootstrapper = PeerTableBootstrapper(config, dnsResolver)
        val next = bootstrapper.getNext(1)
        next.size.toLong() shouldBe 1
    }

    @Test
    fun getNext_whenRequestingMoreThanConfigured() {
        val config: P2pConfiguration = mockk()
        every { config.networkParameters } returns defaultAlphaNetParameters
        every { config.bootstrapPeers } returns listOf("1.1.1.1")
        every { config.bootstrappingDnsSeeds } returns emptyList()
        val dnsResolver: DnsResolver = mockk()
        val bootstrapper = PeerTableBootstrapper(config, dnsResolver)
        val next = bootstrapper.getNext(2)
        next.size.toLong() shouldBe 1
    }
}
