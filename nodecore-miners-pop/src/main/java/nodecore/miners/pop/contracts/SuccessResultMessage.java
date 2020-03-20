// VeriBlock PoP Miner
// Copyright 2017-2020 Xenios SEZC
// All rights reserved.
// https://www.veriblock.org
// Distributed under the MIT software license, see the accompanying
// file LICENSE or http://www.opensource.org/licenses/mit-license.php.

package nodecore.miners.pop.contracts;

import java.util.List;

public class SuccessResultMessage implements ResultMessage {
    @Override
    public String getCode() {
        return "V200";
    }

    @Override
    public String getMessage() {
        return "Success";
    }

    @Override
    public List<String> getDetails() {
        return null;
    }

    @Override
    public boolean isError() {
        return false;
    }
}
