// VeriBlock NodeCore
// Copyright 2017-2020 Xenios SEZC
// All rights reserved.
// https://www.veriblock.org
// Distributed under the MIT software license, see the accompanying
// file LICENSE or http://www.opensource.org/licenses/mit-license.php.

package org.veriblock.core.wallet.serialization;

import org.veriblock.core.wallet.WalletUnreadableException;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;

public interface WalletSerializer {
    StoredWallet read(InputStream inputStream) throws WalletUnreadableException;

    void write(StoredWallet wallet, OutputStream outputStream) throws UnsupportedEncodingException, IOException;
}
