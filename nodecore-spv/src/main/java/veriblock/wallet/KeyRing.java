// VeriBlock Blockchain Project
// Copyright 2017-2018 VeriBlock, Inc
// Copyright 2018-2020 Xenios SEZC
// All rights reserved.
// https://www.veriblock.org
// Distributed under the MIT software license, see the accompanying
// file LICENSE or http://www.opensource.org/licenses/mit-license.php.
package veriblock.wallet;

import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class KeyRing {
    private final Map<String, Key> keys = new HashMap<>();

    public void add(Key key) {
        if (!keys.containsKey(key.getAddress())) {
            keys.put(key.getAddress(), key);
        }
    }

    public Key get(String address) {
        return keys.get(address);
    }

    public Collection<Key> list() {
        return keys.values();
    }

    public void load(List<Key> toLoad) {
        keys.clear();
        for (Key key : toLoad) {
            add(key);
        }
    }

    public boolean contains(String address) {
        return address != null && keys.containsKey(address);

    }

    public int size() {
        return keys.size();
    }
}
