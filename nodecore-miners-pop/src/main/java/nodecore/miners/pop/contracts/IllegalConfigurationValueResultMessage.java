// VeriBlock PoP Miner
// Copyright 2017-2019 Xenios SEZC
// All rights reserved.
// https://www.veriblock.org
// Distributed under the MIT software license, see the accompanying
// file LICENSE or http://www.opensource.org/licenses/mit-license.php.

package nodecore.miners.pop.contracts;

import java.util.Collections;
import java.util.List;

public class IllegalConfigurationValueResultMessage implements ResultMessage {
    private final List<String> details;

    public IllegalConfigurationValueResultMessage(String details) {
        this.details = Collections.singletonList(details);
    }

    @Override
    public String getCode() {
        return "V051";
    }

    @Override
    public String getMessage() {
        return "Illegal configuration value";
    }

    @Override
    public List<String> getDetails() {
        return details;
    }

    @Override
    public boolean isError() {
        return true;
    }
}
