// VeriBlock PoP Miner
// Copyright 2017-2019 Xenios SEZC
// All rights reserved.
// https://www.veriblock.org
// Distributed under the MIT software license, see the accompanying
// file LICENSE or http://www.opensource.org/licenses/mit-license.php.

package nodecore.miners.pop.contracts;

import java.util.List;

public interface Result {
    void fail();

    boolean didFail();

    List<ResultMessage> getMessages();

    void addMessage(String code, String message, String details, boolean error);

    void addMessage(ResultMessage resultMessage);
}
