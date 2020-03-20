// VeriBlock NodeCore
// Copyright 2017-2020 Xenios SEZC
// All rights reserved.
// https://www.veriblock.org
// Distributed under the MIT software license, see the accompanying
// file LICENSE or http://www.opensource.org/licenses/mit-license.php.

package org.veriblock.core.types;

public class Pair<F, S> {
    private F _first;
    private S _second;

    public Pair(F first, S second) {
        _first = first;
        _second = second;
    }

    public F getFirst() {
        return _first;
    }

    public void setFirst(F first) {
        _first = first;
    }

    public S getSecond() {
        return _second;
    }

    public void setSecond(S second) {
        _second = second;
    }
}
