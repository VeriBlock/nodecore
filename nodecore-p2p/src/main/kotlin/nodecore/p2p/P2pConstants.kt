// VeriBlock NodeCore
// Copyright 2017-2021 Xenios SEZC
// All rights reserved.
// https://www.veriblock.org
// Distributed under the MIT software license, see the accompanying
// file LICENSE or http://www.opensource.org/licenses/mit-license.php.
package nodecore.p2p

object P2pConstants {
    val PROGRAM_NAME = P2pConstants::class.java.getPackage().specificationTitle
    val PROGRAM_VERSION = P2pConstants::class.java.getPackage().implementationVersion
    val FULL_PROGRAM_NAME_VERSION = "$PROGRAM_NAME v$PROGRAM_VERSION"
    val PLATFORM = System.getProperty("os.name") + " | " + System.getProperty("java.version")

    const val PEER_TIMEOUT = 300
    const val PEER_REQUEST_TIMEOUT = 15
    const val BLOCKCHAIN_STALE_UPDATE_PERIOD_MS = 15 * 1000 // 15 seconds
    const val PEER_ACKNOWLEDGE_THRESHOLD = 3
    const val PEER_MAX_ADVERTISEMENTS = 50000
    const val PEER_MESSAGE_SIZE_LIMIT = 1024 * 1024 * 4 // 4 MB
    const val PEER_BAN_MESSAGE_SIZE_LIMIT = 1024 * 1024 * 16 // 16 MB
    const val KEYSTONE_BLOCK_INTERVAL = 20; // Keystone blocks are 0, 20, 40

    const val CONCURRENT_TX_REQUESTS = 3
}
