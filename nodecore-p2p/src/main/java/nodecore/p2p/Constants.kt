// VeriBlock NodeCore
// Copyright 2017-2021 Xenios SEZC
// All rights reserved.
// https://www.veriblock.org
// Distributed under the MIT software license, see the accompanying
// file LICENSE or http://www.opensource.org/licenses/mit-license.php.
package nodecore.p2p

object Constants {
    val PROGRAM_NAME = Constants::class.java.getPackage().specificationTitle
    val PROGRAM_VERSION = Constants::class.java.getPackage().implementationVersion
    val FULL_PROGRAM_NAME_VERSION = "$PROGRAM_NAME v$PROGRAM_VERSION"
    val PLATFORM = System.getProperty("os.name") + " | " + System.getProperty("java.version")

    const val PEER_TIMEOUT = 300
    const val PEER_REQUEST_TIMEOUT = 15
    const val BLOCKCHAIN_STALE_UPDATE_PERIOD_MS = 15 * 1000 // 15 seconds
    const val PEER_ACKNOWLEDGE_THRESHOLD = 3
    const val PEER_MAX_ADVERTISEMENTS = 50000
    const val PEER_MESSAGE_SIZE_LIMIT = 1024 * 1024 * 4 // 4 MB
    const val PEER_BAN_MESSAGE_SIZE_LIMIT = 1024 * 1024 * 16 // 16 MB
}
