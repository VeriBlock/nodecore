// VeriBlock Blockchain Project
// Copyright 2017-2018 VeriBlock, Inc
// Copyright 2018-2019 Xenios SEZC
// All rights reserved.
// https://www.veriblock.org
// Distributed under the MIT software license, see the accompanying
// file LICENSE or http://www.opensource.org/licenses/mit-license.php.

package org.veriblock.sdk.auditor;

import java.util.Arrays;
import java.util.Optional;

public enum Operation {
    ADD_BLOCK((short)1),
    SET_HEAD((short)2),
    SET_PROOF((short)4);

    private final short value;

    Operation(final short newValue) {
        value = newValue;
    }

    public short getValue() { return value; }

    public static Optional<Operation> valueOf(short value) {
        return Arrays.stream(values())
                .filter(ops -> ops.value == value)
                .findFirst();
    }
}
