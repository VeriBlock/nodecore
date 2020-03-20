// VeriBlock NodeCore
// Copyright 2017-2020 Xenios SEZC
// All rights reserved.
// https://www.veriblock.org
// Distributed under the MIT software license, see the accompanying
// file LICENSE or http://www.opensource.org/licenses/mit-license.php.

package nodecore.api.ucp.utilities;

import nodecore.api.ucp.arguments.UCPArgument;

import java.util.HashMap;

public final class UCPUtility {
    private UCPUtility(){}

    public static String format(HashMap<String, UCPArgument> toFormat) {
        if (toFormat == null) {
            throw new IllegalArgumentException("format cannot be called with a null toFormat HashMap!");
        }

        String object = "{";
        for (String key : toFormat.keySet()) {
            object += key + ":" + toFormat.toString() + ";";
        }
        object += "}";

        return object;
    }
}

