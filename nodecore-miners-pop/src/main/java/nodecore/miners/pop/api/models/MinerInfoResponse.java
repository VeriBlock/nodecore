// VeriBlock NodeCore
// Copyright 2017-2019 Xenios SEZC
// All rights reserved.
// https://www.veriblock.org
// Distributed under the MIT software license, see the accompanying
// file LICENSE or http://www.opensource.org/licenses/mit-license.php.

package nodecore.miners.pop.api.models;

import java.util.List;

public class MinerInfoResponse {
    public String minerAddress;
    public String bitcoinAddress;
    public long bitcoinBalance;
    public List<String> walletSeed;
}
