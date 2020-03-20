// VeriBlock NodeCore
// Copyright 2017-2020 Xenios SEZC
// All rights reserved.
// https://www.veriblock.org
// Distributed under the MIT software license, see the accompanying
// file LICENSE or http://www.opensource.org/licenses/mit-license.php.

package org.veriblock.core.types;

public class Triple<F, S, T> {
    private F _first;
    private S _second;
    private T _third;

    public Triple(F first, S second, T third) {
        _first = first;
        _second = second;
        _third = third;
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

    public T getThird() {
        return _third;
    }

    public void setThird(T third) {
        _third = third;
    }
}
