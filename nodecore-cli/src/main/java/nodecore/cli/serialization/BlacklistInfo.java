// VeriBlock NodeCore CLI
// Copyright 2017-2020 Xenios SEZC
// All rights reserved.
// https://www.veriblock.org
// Distributed under the MIT software license, see the accompanying
// file LICENSE or http://www.opensource.org/licenses/mit-license.php.

package nodecore.cli.serialization;

import nodecore.api.grpc.VeriBlockMessages;

public class BlacklistInfo {

    public String type;
    public String value;
    public String reason;
    public int timestamp;
    public int expiryTimestamp;

    public BlacklistInfo(VeriBlockMessages.BlackListInfo info) {
        type = info.getType().name();
        reason = info.getReason();
        value = info.getValue();
        timestamp = info.getTimestamp();
        expiryTimestamp = info.getExpiryTimestamp();
    }
}
