// VeriBlock NodeCore
// Copyright 2017-2020 Xenios SEZC
// All rights reserved.
// https://www.veriblock.org
// Distributed under the MIT software license, see the accompanying
// file LICENSE or http://www.opensource.org/licenses/mit-license.php.

package org.veriblock.core.wallet;

public class WalletLockedException extends IllegalStateException {
    public WalletLockedException(String message) {
        super(message);
    }
}
