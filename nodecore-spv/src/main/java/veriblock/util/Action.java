// VeriBlock Blockchain Project
// Copyright 2017-2018 VeriBlock, Inc
// Copyright 2018-2020 Xenios SEZC
// All rights reserved.
// https://www.veriblock.org
// Distributed under the MIT software license, see the accompanying
// file LICENSE or http://www.opensource.org/licenses/mit-license.php.
package veriblock.util;

@FunctionalInterface
public interface Action<T1, T2, T3, T4> {
    void accept(T1 p1, T2 p2, T3 p3, T4 p4);
}
