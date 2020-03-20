// VeriBlock NodeCore
// Copyright 2017-2020 Xenios SEZC
// All rights reserved.
// https://www.veriblock.org
// Distributed under the MIT software license, see the accompanying
// file LICENSE or http://www.opensource.org/licenses/mit-license.php.

package org.veriblock.core.wallet;

public class WalletImportException extends RuntimeException {
    public WalletImportException(String message, Throwable cause) {
        super(message, cause);
    }
}
