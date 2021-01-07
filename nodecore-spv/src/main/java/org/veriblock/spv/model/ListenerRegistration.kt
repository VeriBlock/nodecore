// VeriBlock Blockchain Project
// Copyright 2017-2018 VeriBlock, Inc
// Copyright 2018-2021 Xenios SEZC
// All rights reserved.
// https://www.veriblock.org
// Distributed under the MIT software license, see the accompanying
// file LICENSE or http://www.opensource.org/licenses/mit-license.php.
package org.veriblock.spv.model

import com.google.common.base.Preconditions
import java.util.concurrent.Executor

class ListenerRegistration<T>(
    val listener: T,
    val executor: Executor
) {
    companion object {
        fun <T> removeFromList(listener: T, list: MutableList<ListenerRegistration<T>>): Boolean {
            Preconditions.checkNotNull(listener)
            var item: ListenerRegistration<T>? = null
            for (registration in list) {
                if (registration.listener === listener) {
                    item = registration
                    break
                }
            }
            return item != null && list.remove(item)
        }
    }

}
