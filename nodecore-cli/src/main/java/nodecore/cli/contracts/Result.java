// VeriBlock NodeCore CLI
// Copyright 2017-2019 Xenios SEZC
// All rights reserved.
// https://www.veriblock.org
// Distributed under the MIT software license, see the accompanying
// file LICENSE or http://www.opensource.org/licenses/mit-license.php.

package nodecore.cli.contracts;

import org.joda.time.Interval;

import java.util.ArrayList;

public interface Result {
    void fail();

    boolean didFail();

    Interval getExecutionTime();

    void setExecutionTime(Interval executionTime);

    ArrayList<ResultMessage> getMessages();

    void addMessage(String code, String message, String details, boolean error);
}
