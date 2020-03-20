// VeriBlock NodeCore
// Copyright 2017-2020 Xenios SEZC
// All rights reserved.
// https://www.veriblock.org
// Distributed under the MIT software license, see the accompanying
// file LICENSE or http://www.opensource.org/licenses/mit-license.php.

package org.veriblock.core.contracts;

public interface WalletRepository {
    byte[] getWallet();

    String getWalletConfig();

    String getDefaultAddress();

    void setWallet(byte[] data) throws Exception;

    void setDefaultAddress(String address) throws Exception;
}
