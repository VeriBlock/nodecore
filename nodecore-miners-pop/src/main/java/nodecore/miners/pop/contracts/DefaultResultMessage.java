// VeriBlock PoP Miner
// Copyright 2017-2020 Xenios SEZC
// All rights reserved.
// https://www.veriblock.org
// Distributed under the MIT software license, see the accompanying
// file LICENSE or http://www.opensource.org/licenses/mit-license.php.

package nodecore.miners.pop.contracts;

import java.util.Collections;
import java.util.List;

public class DefaultResultMessage implements ResultMessage {
    private String code;
    private String message;
    private List<String> details;
    private boolean error;

    public DefaultResultMessage(String code, String message, String details, boolean error) {
        this.code = code;
        this.message = message;
        this.details = Collections.singletonList(details);
        this.error = error;
    }

    public DefaultResultMessage(String code, String message, List<String> details, boolean error) {
        this.code = code;
        this.message = message;
        this.details = details;
        this.error = error;
    }

    @Override
    public String getCode() {
        return code;
    }

    @Override
    public String getMessage() {
        return message;
    }

    @Override
    public List<String> getDetails() {
        return details;
    }

    @Override
    public boolean isError() {
        return error;
    }
}
