// VeriBlock Blockchain Project
// Copyright 2017-2018 VeriBlock, Inc
// Copyright 2018-2019 Xenios SEZC
// All rights reserved.
// https://www.veriblock.org
// Distributed under the MIT software license, see the accompanying
// file LICENSE or http://www.opensource.org/licenses/mit-license.php.

package org.veriblock.sdk.alt

import java.util.*

sealed class SubmitResponse {
    abstract val isSuccess: Boolean
}

class SuccessResponse : SubmitResponse() {
    override val isSuccess: Boolean = true

    override fun toString() = "Success"
}

class FailureResponse : SubmitResponse() {
    override val isSuccess: Boolean = false

    private val failureReasons: MutableList<String> = ArrayList()

    fun getFailureReasons(): List<String> {
        return Collections.unmodifiableList(failureReasons)
    }

    fun addFailureReason(reason: String) {
        failureReasons.add(reason)
    }

    override fun toString() = "Failure" + if (failureReasons.isNotEmpty()) {
        ":\n" + failureReasons.joinToString("\n") { "\t$it" }
    } else {
        ""
    }
}
