// VeriBlock NodeCore CLI
// Copyright 2017-2019 Xenios SEZC
// All rights reserved.
// https://www.veriblock.org
// Distributed under the MIT software license, see the accompanying
// file LICENSE or http://www.opensource.org/licenses/mit-license.php.

package nodecore.cli.commands.serialization;

import nodecore.api.grpc.WhiteListInfo;

public class WhitelistInfo {

    public String type;
    public String value;
    public int timestamp;

    public WhitelistInfo(WhiteListInfo info) {
        type = info.getType().name();
        value = info.getValue();
        timestamp = info.getTimestamp();
    }
}
