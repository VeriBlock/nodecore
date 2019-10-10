// VeriBlock Blockchain Project
// Copyright 2017-2018 VeriBlock, Inc
// Copyright 2018-2019 Xenios SEZC
// All rights reserved.
// https://www.veriblock.org
// Distributed under the MIT software license, see the accompanying
// file LICENSE or http://www.opensource.org/licenses/mit-license.php.

package org.veriblock.miners.pop.securityinheriting

import com.google.common.util.concurrent.SettableFuture
import org.veriblock.lite.util.Threading
import org.veriblock.miners.pop.Miner
import org.veriblock.sdk.alt.SecurityInheritingChain
import org.veriblock.sdk.checkSuccess
import org.veriblock.sdk.createLogger
import java.util.concurrent.ScheduledFuture
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicBoolean

private val logger = createLogger {}

class SecurityInheritingAutoMiner(
    private val miner: Miner,
    private val chainId: String,
    private val chain: SecurityInheritingChain
) {
    private val healthy = AtomicBoolean(false)
    private val connected = SettableFuture.create<Boolean>()

    private var bestBlockHeight: Int = -1

    private var pollSchedule: ScheduledFuture<*>? = null

    fun start() {
        pollSchedule = Threading.SI_POLL_THREAD.scheduleWithFixedDelay({
            this.poll()
        }, 1L, 1L, TimeUnit.SECONDS)
    }

    fun stop() {
        pollSchedule?.cancel(false)
        pollSchedule = null
    }

    private fun poll() {
        try {
            if (healthy.get()) {
                val bestBlockHeight: Int = try {
                    chain.getBestBlockHeight()
                } catch (e: Exception) {
                    logger.error("$chainId Chain Error", e)
                    healthy.set(false)
                    return
                }

                if (this.bestBlockHeight == -1 || bestBlockHeight != this.bestBlockHeight) {
                    logger.debug { "New chain head detected!" }
                    if (chain.shouldAutoMine(bestBlockHeight)) {
                        miner.mine(chainId, bestBlockHeight)
                    }
                    this.bestBlockHeight = bestBlockHeight
                }
            } else {
                val pinged = checkSuccess { chain.getBestBlockHeight() }
                if (pinged) {
                    logger.info("Connected to SI Chain ($chainId)")
                    healthy.set(true)
                    connected.set(true)
                }
            }
        } catch (e: Exception) {
            logger.error(e) { "Error when polling SI Chain ($chainId)" }
        }
    }
}
