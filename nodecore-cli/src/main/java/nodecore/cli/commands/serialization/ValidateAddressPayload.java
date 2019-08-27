// VeriBlock NodeCore CLI
// Copyright 2017-2019 Xenios SEZC
// All rights reserved.
// https://www.veriblock.org
// Distributed under the MIT software license, see the accompanying
// file LICENSE or http://www.opensource.org/licenses/mit-license.php.

package nodecore.cli.commands.serialization;

import com.google.gson.annotations.SerializedName;
import nodecore.api.grpc.ValidateAddressReply;
import nodecore.api.grpc.utilities.ByteStringAddressUtility;
import nodecore.api.grpc.utilities.ByteStringUtility;

public class ValidateAddressPayload {
    public ValidateAddressPayload(final ValidateAddressReply reply) {
        isRemote = reply.getIsRemote();
        address = ByteStringAddressUtility.parseProperAddressTypeAutomatically(reply.getAddress());
        publicKey = ByteStringUtility.byteStringToHex(reply.getPublicKey());
    }

    public String address;

    @SerializedName("is_remote")
    public boolean isRemote;

    @SerializedName("public_key")
    public String publicKey;
}
