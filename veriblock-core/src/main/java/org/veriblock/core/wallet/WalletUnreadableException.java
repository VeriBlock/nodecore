// VeriBlock NodeCore
// Copyright 2017-2020 Xenios SEZC
// All rights reserved.
// https://www.veriblock.org
// Distributed under the MIT software license, see the accompanying
// file LICENSE or http://www.opensource.org/licenses/mit-license.php.

package org.veriblock.core.wallet;

import java.io.IOException;

public class WalletUnreadableException extends IOException {
    public WalletUnreadableException(String message) {
        super(message);
    }

    public WalletUnreadableException(String message, Throwable cause) {
        super(message, cause);
    }
}
