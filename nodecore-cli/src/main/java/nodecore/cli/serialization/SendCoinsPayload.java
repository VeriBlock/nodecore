// VeriBlock NodeCore CLI
// Copyright 2017-2020 Xenios SEZC
// All rights reserved.
// https://www.veriblock.org
// Distributed under the MIT software license, see the accompanying
// file LICENSE or http://www.opensource.org/licenses/mit-license.php.

package nodecore.cli.serialization;

import com.google.gson.annotations.SerializedName;
import nodecore.api.grpc.VeriBlockMessages;
import nodecore.api.grpc.utilities.ByteStringUtility;

public class SendCoinsPayload {
    public SendCoinsPayload(final VeriBlockMessages.SendCoinsReply reply) {
        txids = new String[reply.getTxIdsCount()];

        for (int i = 0; i < reply.getTxIdsCount(); i++) {
            txids[i] = ByteStringUtility.byteStringToHex(reply.getTxIds(i));
        }
    }

    @SerializedName("txids")
    public String[] txids;
}
