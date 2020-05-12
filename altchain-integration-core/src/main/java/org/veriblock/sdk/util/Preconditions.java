// VeriBlock Blockchain Project
// Copyright 2017-2018 VeriBlock, Inc
// Copyright 2018-2019 Xenios SEZC
// All rights reserved.
// https://www.veriblock.org
// Distributed under the MIT software license, see the accompanying
// file LICENSE or http://www.opensource.org/licenses/mit-license.php.

package org.veriblock.sdk.util;

import java.util.function.Supplier;

public class Preconditions {
    public static <T> void notNull(T object, String errorMessage) {
        if (object == null) throw new NullPointerException(errorMessage);
    }

    public static void state(boolean state, String errorMessage) {
        if (!state) throw new IllegalStateException(errorMessage);
    }

    public static <T> void argument(boolean state, String errorMessage) {
        if (!state) throw new IllegalArgumentException(errorMessage);
    }

    /**
     * Special signature for the cases in which we don't want the error message
     * to be computed at every call, but only whenever the check fails
     */
    public static <T> void argument(boolean state, Supplier<String> errorMessageSupplier) {
        if (!state) throw new IllegalArgumentException(errorMessageSupplier.get());
    }
}
