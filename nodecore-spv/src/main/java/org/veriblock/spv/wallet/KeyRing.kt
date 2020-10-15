// VeriBlock Blockchain Project
// Copyright 2017-2018 VeriBlock, Inc
// Copyright 2018-2020 Xenios SEZC
// All rights reserved.
// https://www.veriblock.org
// Distributed under the MIT software license, see the accompanying
// file LICENSE or http://www.opensource.org/licenses/mit-license.php.
package org.veriblock.spv.wallet

import java.util.HashMap

class KeyRing {
    private val keys: MutableMap<String, Key> = HashMap()
    fun add(key: Key) {
        if (!keys.containsKey(key.address)) {
            keys[key.address] = key
        }
    }

    operator fun get(address: String): Key? {
        return keys[address]
    }

    fun list(): Collection<Key> {
        return keys.values
    }

    fun load(toLoad: List<Key>) {
        keys.clear()
        for (key in toLoad) {
            add(key)
        }
    }

    operator fun contains(address: String?): Boolean {
        return address != null && keys.containsKey(address)
    }

    fun size(): Int {
        return keys.size
    }
}
