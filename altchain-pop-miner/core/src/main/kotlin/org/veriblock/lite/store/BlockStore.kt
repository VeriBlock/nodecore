// VeriBlock Blockchain Project
// Copyright 2017-2018 VeriBlock, Inc
// Copyright 2018-2019 Xenios SEZC
// All rights reserved.
// https://www.veriblock.org
// Distributed under the MIT software license, see the accompanying
// file LICENSE or http://www.opensource.org/licenses/mit-license.php.

package org.veriblock.lite.store

import org.veriblock.sdk.BlockStoreException

interface BlockStore<T, U> {
    fun getChainHead(): U?

    @Throws(BlockStoreException::class)
    fun setChainHead(chainHead: U): U?

    @Throws(BlockStoreException::class)
    fun put(storedBlock: U): U

    @Throws(BlockStoreException::class)
    fun replace(hash: T, storedBlock: U): U?

    @Throws(BlockStoreException::class)
    fun get(hash: T): U?

    @Throws(BlockStoreException::class)
    fun get(hash: T, count: Int): List<U>

    fun getFromChain(hash: T, blocksAgo: Int): U?

    fun scanBestChain(hash: T): U?
}
