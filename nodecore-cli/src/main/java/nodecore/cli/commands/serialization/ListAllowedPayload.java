// VeriBlock NodeCore CLI
// Copyright 2017-2019 Xenios SEZC
// All rights reserved.
// https://www.veriblock.org
// Distributed under the MIT software license, see the accompanying
// file LICENSE or http://www.opensource.org/licenses/mit-license.php.

package nodecore.cli.commands.serialization;

import com.google.gson.annotations.SerializedName;
import nodecore.api.grpc.ListAllowedReply;

import java.util.List;
import java.util.stream.Collectors;

public class ListAllowedPayload {

    @SerializedName("allowed")
    public List<WhitelistInfo> allowed;

    public ListAllowedPayload(final ListAllowedReply reply) {
        allowed = reply.getEntriesList().stream().map(info -> new WhitelistInfo(info)).collect(Collectors.toList());
    }
}
