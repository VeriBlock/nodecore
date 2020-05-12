// VeriBlock Blockchain Project
// Copyright 2017-2018 VeriBlock, Inc
// Copyright 2018-2019 Xenios SEZC
// All rights reserved.
// https://www.veriblock.org
// Distributed under the MIT software license, see the accompanying
// file LICENSE or http://www.opensource.org/licenses/mit-license.php.

package org.veriblock.sdk.models;

public class Pair<S, T> {
    private final S first;
    private final T second;

    public Pair(S s, T t) {
        first = s;
        second = t;
    }
    
    public S getFirst() { return first; }
    public T getSecond() { return second; }
}
