// VeriBlock PoP Miner
// Copyright 2017-2019 Xenios SEZC
// All rights reserved.
// https://www.veriblock.org
// Distributed under the MIT software license, see the accompanying
// file LICENSE or http://www.opensource.org/licenses/mit-license.php.

package nodecore.miners.pop.contracts;

import java.util.ArrayList;

public class DefaultResult implements Result {
    private boolean failed;
    private ArrayList<ResultMessage> messages;

    public DefaultResult() {
        messages = new ArrayList<>();
    }

    @Override
    public void fail() { failed = true; }

    @Override
    public boolean didFail() {
        return failed;
    }

    @Override
    public ArrayList<ResultMessage> getMessages() {
        return messages;
    }

    @Override
    public void addMessage(String code, String message, String details, boolean error) {
        messages.add(new DefaultResultMessage(code, message, details, error));
    }

    @Override
    public void addMessage(ResultMessage resultMessage) {
        messages.add(resultMessage);
    }
}
