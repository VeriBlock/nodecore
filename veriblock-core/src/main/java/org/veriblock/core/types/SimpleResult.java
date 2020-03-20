// VeriBlock NodeCore
// Copyright 2017-2020 Xenios SEZC
// All rights reserved.
// https://www.veriblock.org
// Distributed under the MIT software license, see the accompanying
// file LICENSE or http://www.opensource.org/licenses/mit-license.php.

package org.veriblock.core.types;

public class SimpleResult {
    private String errorString;
    private boolean success;

    public SimpleResult(boolean success, String error) {
        this.success = success;
        this.errorString = error;
    }

    public String getError() {
        return errorString;
    }

    public boolean wasSuccessful() {
        return success;
    }
}
