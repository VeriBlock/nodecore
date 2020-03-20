// VeriBlock Blockchain Project
// Copyright 2017-2018 VeriBlock, Inc
// Copyright 2018-2020 Xenios SEZC
// All rights reserved.
// https://www.veriblock.org
// Distributed under the MIT software license, see the accompanying
// file LICENSE or http://www.opensource.org/licenses/mit-license.php.

package org.veriblock.shell.core

import java.util.*

class Result(
    val isFailed: Boolean
) {
    private val messages: ArrayList<ResultMessage> = ArrayList()

    fun getMessages(): List<ResultMessage> {
        return messages
    }

    fun addMessage(code: String, message: String, details: String, error: Boolean = isFailed) {
        messages.add(ResultMessage(code, message, details, error))
    }

    fun addMessage(code: String, message: String, details: List<String>, error: Boolean = isFailed) {
        messages.add(ResultMessage(code, message, details, error))
    }

    fun addMessage(resultMessage: ResultMessage) {
        messages.add(resultMessage)
    }
}

inline fun success(builder: Result.() -> Unit = {}) = Result(false).apply { builder() }
inline fun failure(builder: Result.() -> Unit = {}) = Result(true).apply { builder() }

fun success(code: String, message: String, details: String) = success {
    addMessage(code, message, details, true)
}

fun failure(code: String, message: String, details: String) = failure {
    addMessage(code, message, details, true)
}

