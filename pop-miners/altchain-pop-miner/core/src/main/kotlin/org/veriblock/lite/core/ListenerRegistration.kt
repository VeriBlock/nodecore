// VeriBlock Blockchain Project
// Copyright 2017-2018 VeriBlock, Inc
// Copyright 2018-2020 Xenios SEZC
// All rights reserved.
// https://www.veriblock.org
// Distributed under the MIT software license, see the accompanying
// file LICENSE or http://www.opensource.org/licenses/mit-license.php.

package org.veriblock.lite.core

import java.util.concurrent.Executor

class ListenerRegistration<T>(
    val listener: T,
    val executor: Executor
)

fun <T> MutableList<out ListenerRegistration<T>>.removeListener(listener: T): Boolean =
    removeIf { it.listener === listener }
