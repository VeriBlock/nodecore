// VeriBlock Blockchain Project
// Copyright 2017-2018 VeriBlock, Inc
// Copyright 2018-2020 Xenios SEZC
// All rights reserved.
// https://www.veriblock.org
// Distributed under the MIT software license, see the accompanying
// file LICENSE or http://www.opensource.org/licenses/mit-license.php.
package veriblock.util;

import java.util.function.Predicate;
import java.util.function.Supplier;

public class Checks {
    public static <T> void notNull(T object) {
        if (object == null) throw new NullPointerException();
    }

    public static void state(Supplier<Boolean> predicate) {
        if (predicate == null || !predicate.get()) throw new IllegalStateException();
    }

    public static <T> void argument(T arg, Predicate<T> predicate) {
        if (predicate == null || !predicate.test(arg)) throw new IllegalArgumentException();
    }
}
