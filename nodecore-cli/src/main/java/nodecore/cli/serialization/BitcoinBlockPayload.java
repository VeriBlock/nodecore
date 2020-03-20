package nodecore.cli.serialization;

// VeriBlock NodeCore CLI
// Copyright 2017-2020 Xenios SEZC
// All rights reserved.
// https://www.veriblock.org
// Distributed under the MIT software license, see the accompanying
// file LICENSE or http://www.opensource.org/licenses/mit-license.php.

import com.google.gson.annotations.SerializedName;
import nodecore.api.grpc.VeriBlockMessages;
import nodecore.api.grpc.utilities.ByteStringUtility;

public class BitcoinBlockPayload {

    public BitcoinBlockPayload(final VeriBlockMessages.GetLastBitcoinBlockReply reply) {
        header = ByteStringUtility.byteStringToHex(reply.getHeader());
        height = reply.getHeight();
        hash = ByteStringUtility.byteStringToHex(reply.getHash());
    }

    @SerializedName("header")
    public String header;

    @SerializedName("height")
    public int height;

    @SerializedName("hash")
    public String hash;

    public String getHeader() {
        return header;
    }

    public int getHeight() {
        return height;
    }

    public String getHash() {
        return hash;
    }
}
