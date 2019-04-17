// VeriBlock NodeCore CLI
// Copyright 2017-2019 Xenios SEZC
// All rights reserved.
// https://www.veriblock.org
// Distributed under the MIT software license, see the accompanying
// file LICENSE or http://www.opensource.org/licenses/mit-license.php.

package nodecore.cli.contracts;

import org.joda.time.Interval;

import java.util.ArrayList;

public class DefaultResult implements Result {
    private boolean _failed;
    private Interval _executionTime;
    private ArrayList<ResultMessage> _messages;

    public DefaultResult() {
        _messages = new ArrayList<>();
    }

    @Override
    public void fail() { _failed = true; }

    @Override
    public boolean didFail() {
        return _failed;
    }

    @Override
    public Interval getExecutionTime() {
        return _executionTime;
    }

    @Override
    public void setExecutionTime(Interval executionTime) {
        _executionTime = executionTime;
    }

    @Override
    public ArrayList<ResultMessage> getMessages() {
        return _messages;
    }

    @Override
    public void addMessage(String code, String message, String details, boolean error) {
        _messages.add(new DefaultResultMessage(code, message, details, error));
    }
}
