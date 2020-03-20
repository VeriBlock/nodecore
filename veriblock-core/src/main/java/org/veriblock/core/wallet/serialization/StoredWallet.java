// VeriBlock NodeCore
// Copyright 2017-2020 Xenios SEZC
// All rights reserved.
// https://www.veriblock.org
// Distributed under the MIT software license, see the accompanying
// file LICENSE or http://www.opensource.org/licenses/mit-license.php.

package org.veriblock.core.wallet.serialization;

import java.util.List;

public class StoredWallet {
    public int version;
    public int keyType;

    public boolean locked;

    public String defaultAddress;

    public List<StoredAddress> addresses;
}
