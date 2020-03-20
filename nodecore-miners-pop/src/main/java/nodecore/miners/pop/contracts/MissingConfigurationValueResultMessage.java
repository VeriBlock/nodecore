// VeriBlock PoP Miner
// Copyright 2017-2020 Xenios SEZC
// All rights reserved.
// https://www.veriblock.org
// Distributed under the MIT software license, see the accompanying
// file LICENSE or http://www.opensource.org/licenses/mit-license.php.

package nodecore.miners.pop.contracts;

import java.util.Collections;
import java.util.List;

public class MissingConfigurationValueResultMessage implements ResultMessage {

    private final String propertyName;

    public MissingConfigurationValueResultMessage(String propertyName) {
        this.propertyName = propertyName;
    }

    @Override
    public String getCode() {
        return "V050";
    }

    @Override
    public String getMessage() {
        return "Missing configuration value";
    }

    @Override
    public List<String> getDetails() {
        return Collections.singletonList(String.format("A value is required for property '%s'", propertyName));
    }

    @Override
    public boolean isError() {
        return true;
    }
}
