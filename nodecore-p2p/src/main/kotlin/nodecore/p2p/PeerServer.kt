// VeriBlock NodeCore
// Copyright 2017-2021 Xenios SEZC
// All rights reserved.
// https://www.veriblock.org
// Distributed under the MIT software license, see the accompanying
// file LICENSE or http://www.opensource.org/licenses/mit-license.php.
package nodecore.p2p

import io.ktor.network.sockets.aSocket
import io.ktor.util.network.NetworkAddress
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import org.veriblock.core.utilities.createLogger
import java.util.concurrent.atomic.AtomicBoolean

private val logger = createLogger {}

class PeerServer(
    configuration: P2pConfiguration,
    private val peerTable: PeerTable
) {
    private val bindAddress = NetworkAddress(configuration.peerBindAddress, configuration.peerBindPort)

    private val running = AtomicBoolean(false)

    fun start() {
        running.set(true)
        CoroutineScope(peerTable.socketDispatcher).launch {
            run()
        }
    }
    
    private suspend fun run() {
        try {
            val serverSocket = aSocket(peerTable.selectorManager).tcp().bind(bindAddress)
            while (running.get()) {
                try {
                    val socket = serverSocket.accept()
                    peerTable.registerIncomingConnection(socket)
                } catch (t: Throwable) {
                    logger.error(t) { "Unable to accept incoming socket connection at $bindAddress!" }
                }
            }
        } catch (e: CancellationException) {
            logger.info { "Peer Server coroutine cancelled! Shutting down..." }
        } catch (t: Throwable) {
            logger.error(t) { "Unable to create server socket and bind it to $bindAddress!" }
        }
    }
    
    fun stop() {
        running.set(false)
        Threading.PEER_SERVER_THREAD.safeShutdown()
        Threading.PEER_IO_POOL.safeShutdown()
    }
}
