// VeriBlock Blockchain Project
// Copyright 2017-2018 VeriBlock, Inc
// Copyright 2018-2020 Xenios SEZC
// All rights reserved.
// https://www.veriblock.org
// Distributed under the MIT software license, see the accompanying
// file LICENSE or http://www.opensource.org/licenses/mit-license.php.

package org.veriblock.lite.core

import org.veriblock.sdk.models.VeriBlockPublication

class PublicationSubscription(
    val keystoneHash: String,
    val contextHash: String,
    val btcContextHash: String,
    private val listener: (List<VeriBlockPublication>) -> Unit,
    private val failureListener: (Throwable) -> Unit
) {
    var results: List<VeriBlockPublication>? = null
        private set

    private fun setResults(results: List<VeriBlockPublication>) {
        this.results = results
        listener(results)
    }

    fun trySetResults(publications: List<VeriBlockPublication>): Boolean {
        if (publications.isEmpty()) {
            return false
        }

        val results = this.results
        if (results == null || results.size != publications.size) {
            setResults(publications)
            return true
        }

        for (i in publications.indices) {
            if (results[i].transaction.id != publications[i].transaction.id) {
                setResults(publications)
                return true
            }
        }

        return false
    }

    fun onError(e: Exception) {
        failureListener(e)
    }
}
