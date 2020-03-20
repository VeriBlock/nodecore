// VeriBlock NodeCore
// Copyright 2017-2020 Xenios SEZC
// All rights reserved.
// https://www.veriblock.org
// Distributed under the MIT software license, see the accompanying
// file LICENSE or http://www.opensource.org/licenses/mit-license.php.

package nodecore.cli.serialization;

import com.google.gson.annotations.SerializedName;
import nodecore.api.grpc.VeriBlockMessages;

import java.util.List;

public class ListBannedMinersPayload {

    @SerializedName("banned_clients")
    public List<String> bannedClients;

    public ListBannedMinersPayload(final VeriBlockMessages.ListBannedMinersReply reply) {
        bannedClients = reply.getClientsList();
    }
}
