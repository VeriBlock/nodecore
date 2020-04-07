// VeriBlock PoP Miner
// Copyright 2017-2020 Xenios SEZC
// All rights reserved.
// https://www.veriblock.org
// Distributed under the MIT software license, see the accompanying
// file LICENSE or http://www.opensource.org/licenses/mit-license.php.
package org.veriblock.miners.pop

import io.grpc.Metadata

object Constants {
    val APPLICATION_NAME: String? = Constants::class.java.getPackage().specificationTitle
    val APPLICATION_VERSION: String? = Constants::class.java.getPackage().specificationVersion

    val FULL_APPLICATION_NAME_VERSION = "$APPLICATION_NAME v$APPLICATION_VERSION"
    const val DEFAULT_DATA_FILE = "veriblock-pop-miner.db"
    val RPC_PASSWORD_HEADER_NAME = Metadata.Key.of(
        "X-VBK-RPC-PASSWORD", Metadata.ASCII_STRING_MARSHALLER
    )
    const val WALLET_SEED_VIEWED_KEY = "wallet.seed.viewed"
    const val DEFAULT_WALLET_CREATION_DATE = 1514764800L
    const val ESTIMATED_POP_SIZE = 283L
    const val BITCOIN_KB: Long = 1000
    const val POP_SETTLEMENT_INTERVAL = 400
    const val MEMPOOL_CHAIN_LIMIT = 24
}
