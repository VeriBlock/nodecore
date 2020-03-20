// VeriBlock NodeCore
// Copyright 2017-2020 Xenios SEZC
// All rights reserved.
// https://www.veriblock.org
// Distributed under the MIT software license, see the accompanying
// file LICENSE or http://www.opensource.org/licenses/mit-license.php.

package org.veriblock.core.contracts;

import java.io.IOException;
import java.io.OutputStream;

public interface Output {
    TransactionAmount getAmount();

    TransactionAddress getAddress();

    String getAddressString();

    void serializeToStream(OutputStream stream) throws IOException;
}
