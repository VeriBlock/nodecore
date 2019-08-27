// VeriBlock NodeCore
// Copyright 2017-2019 Xenios SEZC
// All rights reserved.
// https://www.veriblock.org
// Distributed under the MIT software license, see the accompanying
// file LICENSE or http://www.opensource.org/licenses/mit-license.php.

package nodecore.cli.commands.serialization;

import com.google.gson.annotations.SerializedName;
import nodecore.api.grpc.ListBannedReply;

import java.util.List;
import java.util.stream.Collectors;

public class ListBannedPayload {

    @SerializedName("banned_peers")
    public List<BlacklistInfo> bannedPeers;

    public ListBannedPayload(final ListBannedReply reply) {
        bannedPeers = reply.getEntriesList().stream().map(BlacklistInfo::new).collect(Collectors.toList());
    }
}
