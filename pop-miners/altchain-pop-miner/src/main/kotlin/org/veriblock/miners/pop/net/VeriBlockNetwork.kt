// VeriBlock Blockchain Project
// Copyright 2017-2018 VeriBlock, Inc
// Copyright 2018-2021 Xenios SEZC
// All rights reserved.
// https://www.veriblock.org
// Distributed under the MIT software license, see the accompanying
// file LICENSE or http://www.opensource.org/licenses/mit-license.php.

package org.veriblock.miners.pop.net

import com.google.common.util.concurrent.ListenableFuture
import com.google.common.util.concurrent.SettableFuture
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.time.withTimeout
import org.veriblock.core.utilities.createLogger
import org.veriblock.core.utilities.debugError
import org.veriblock.core.contracts.Balance
import org.veriblock.core.crypto.AnyVbkHash
import org.veriblock.core.utilities.debugWarn
import org.veriblock.miners.pop.core.ApmContext
import org.veriblock.miners.pop.transactionmonitor.TransactionMonitor
import org.veriblock.miners.pop.util.Threading
import org.veriblock.miners.pop.EventBus
import org.veriblock.sdk.models.StateInfo
import org.veriblock.core.wallet.AddressManager
import org.veriblock.miners.pop.MinerConfig
import org.veriblock.miners.pop.util.formatCoinAmount
import org.veriblock.sdk.models.VeriBlockBlock
import org.veriblock.sdk.models.VeriBlockPublication
import org.veriblock.sdk.models.VeriBlockTransaction
import org.veriblock.sdk.models.getSynchronizedMessage
import org.veriblock.spv.util.SpvEventBus
import java.time.Duration
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicBoolean

private val logger = createLogger {}

class VeriBlockNetwork(
    private val config: MinerConfig,
    private val context: ApmContext,
    private val gateway: SpvGateway,
    private val transactionMonitor: TransactionMonitor,
    private val addressManager: AddressManager
) {
    private var firstPoll: Boolean = true

    private val ready = AtomicBoolean(false)
    private val accessible = AtomicBoolean(false)
    private val synchronized = AtomicBoolean(false)
    private val sufficientFunds = AtomicBoolean(false)
    private val connected = SettableFuture.create<Boolean>()

    var latestBalance: Balance = Balance()
    var latestSpvStateInfo: StateInfo = StateInfo()

    fun isSufficientFunded(): Boolean =
        sufficientFunds.get()

    fun isReady(): Boolean =
        ready.get()

    fun isAccessible(): Boolean =
        accessible.get()

    fun isSynchronized(): Boolean =
        synchronized.get()

    fun startAsync(): ListenableFuture<Boolean> {
        Threading.SPV_POLL_THREAD.scheduleWithFixedDelay({
            this.poll()
        }, 1L, 1L, TimeUnit.SECONDS)

        return connected
    }

    fun shutdown() {
        gateway.shutdown()
    }

    suspend fun submitEndorsement(publicationData: ByteArray, feePerByte: Long, maxFee: Long): VeriBlockTransaction {
        val transaction = gateway.submitEndorsementTransaction(
            publicationData, addressManager, feePerByte, maxFee
        )
        transactionMonitor.commitTransaction(transaction)
        return transaction
    }

    fun getBlock(hash: AnyVbkHash): VeriBlockBlock? {
        return gateway.getBlock(hash)
    }

    private fun poll() {
        try {
            var nodeCoreStateInfo: StateInfo? = null
            // Verify if we can make a connection with NodeCore
            if (gateway.isConnected()) {
                // At this point the APM<->NodeCore connection is fine
                if (!isAccessible()) {
                    accessible.set(true)
                }
                // Get the latest stats from NodeCore
                nodeCoreStateInfo = gateway.getSpvStateInfo()
                latestSpvStateInfo = nodeCoreStateInfo
                // Get the latest balance from NodeCore
                val balance = gateway.getBalance()
                if (balance.confirmedBalance != latestBalance.confirmedBalance) {
                    EventBus.balanceChangeEvent.trigger(balance)
                }
                latestBalance = balance

                // Verify the balance
                if (latestBalance.confirmedBalance.atomicUnits > config.maxFee) {
                    if (!isSufficientFunded()) {
                        sufficientFunds.set(true)
                        EventBus.sufficientBalanceEvent.trigger(latestBalance)
                    }
                } else {
                    if (isSufficientFunded()) {
                        sufficientFunds.set(false)
                        EventBus.insufficientBalanceEvent.trigger()
                        logger.info {"""
                            PoP wallet does not contain sufficient funds, 
                            Current balance: ${latestBalance.confirmedBalance.atomicUnits.formatCoinAmount()} ${context.vbkTokenName},
                            Minimum required: ${config.maxFee.formatCoinAmount()}, need ${(config.maxFee - latestBalance.confirmedBalance.atomicUnits).formatCoinAmount()} more
                            Send ${context.vbkTokenName} coins to: ${addressManager.defaultAddress.hash}
                        """.trimIndent()}
                    }
                }

                connected.set(true)

                // Verify the NodeCore synchronization status
                if (nodeCoreStateInfo.isSynchronized) {
                    if (!isSynchronized()) {
                        synchronized.set(true)
                        logger.info { "SPV is synchronized: ${nodeCoreStateInfo.getSynchronizedMessage()}" }
                    }
                } else {
                    if (isSynchronized() || firstPoll) {
                        synchronized.set(false)
                        logger.info { "SPV is not synchronized: ${nodeCoreStateInfo.getSynchronizedMessage()}" }
                    }
                }
            } else {
                // At this point the APM<->NodeCore connection can't be established
                latestSpvStateInfo = StateInfo()
                latestBalance = Balance()
                accessible.set(false)
                synchronized.set(false)
                if (isSufficientFunded()) {
                    sufficientFunds.set(false)
                    EventBus.insufficientBalanceEvent.trigger()
                }
                if (isReady()) {
                    ready.set(false)
                    EventBus.spvNotReadyEvent.trigger()
                }
            }
            if (isAccessible() && isSynchronized()) {
                if (!isReady()) {
                    ready.set(true)
                    EventBus.spvReadyEvent.trigger()
                }
                // At this point the APM<->NodeCore connection is fine and the NodeCore is synchronized so
                // APM can continue with its work
                try {
                    gateway.getLastBlock()
                } catch (e: Exception) {
                    logger.debugWarn(e) { "Unable to get the last VBK block from SPV" }
                    latestSpvStateInfo = StateInfo()
                    latestBalance = Balance()
                    accessible.set(false)
                    synchronized.set(false)
                    if (isSufficientFunded()) {
                        sufficientFunds.set(false)
                        EventBus.insufficientBalanceEvent.trigger()
                    }
                    if (isReady()) {
                        ready.set(false)
                        EventBus.spvNotReadyEvent.trigger()
                    }
                    return
                }
            } else {
                if (isReady()) {
                    ready.set(false)
                    EventBus.spvNotReadyEvent.trigger()
                }
                if (!isAccessible()) {
                    logger.debug { "Unable to connect to peers" }
                } else {
                    nodeCoreStateInfo?.let {
                        if (!isSynchronized()) {
                            if (nodeCoreStateInfo.networkHeight != 0) {
                                logger.debug { it.getSynchronizedMessage() }
                            } else {
                                logger.debug { "Still not connected to the network" }
                            }
                        }
                    }
                }
            }
        } catch (e: Exception) {
            logger.debugError(e) { "Error when polling SPV" }
        }
        firstPoll = false
    }

    suspend fun getVeriBlockPublications(
        keystoneHash: String,
        contextHash: String,
        btcContextHash: String
    ): List<VeriBlockPublication> = withTimeout(Duration.ofMinutes(40)) {
        val extraLogData = """
                |   - Keystone Hash: $keystoneHash
                |   - VBK Context Hash: $contextHash
                |   - BTC Context Hash: $btcContextHash""".trimMargin()
        logger.debug("Waiting for VTBs...\n$extraLogData")
        try {
            // Loop through each new block until we get a not-empty publication list
            SpvEventBus.newBlockFlow.map {
                // Retrieve VTBs from NodeCore
                gateway.getVeriBlockPublications(
                    keystoneHash, contextHash, btcContextHash
                )
            }.first {
                // If the list is not empty, return it
                it.isNotEmpty()
            }
        } catch (e: Exception) {
            try {
                val lastBlock = gateway.getLastBlock()
                logger.info { "Current last block: ${lastBlock.hash} @ ${lastBlock.height}" }
            } catch (ignored: Exception) {
            }
            throw RuntimeException("Error while retrieving VTBs!\n$extraLogData", e)
        }
    }
}
