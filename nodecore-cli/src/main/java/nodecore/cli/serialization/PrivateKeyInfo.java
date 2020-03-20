// VeriBlock NodeCore CLI
// Copyright 2017-2020 Xenios SEZC
// All rights reserved.
// https://www.veriblock.org
// Distributed under the MIT software license, see the accompanying
// file LICENSE or http://www.opensource.org/licenses/mit-license.php.

package nodecore.cli.serialization;

import com.google.gson.annotations.SerializedName;
import nodecore.api.grpc.VeriBlockMessages;
import nodecore.api.grpc.utilities.ByteStringAddressUtility;
import nodecore.api.grpc.utilities.ByteStringUtility;

public class PrivateKeyInfo {
    public PrivateKeyInfo(final VeriBlockMessages.DumpPrivateKeyReply reply) {
        this.address = ByteStringAddressUtility.parseProperAddressTypeAutomatically(reply.getAddress());
        this.privateKey = ByteStringUtility.byteStringToHex(reply.getPrivateKey());
    }

    @SerializedName("address")
    public String address;

    @SerializedName("private_key")
    public String privateKey;
}
