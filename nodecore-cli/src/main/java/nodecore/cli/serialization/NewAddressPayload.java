// VeriBlock NodeCore
// Copyright 2017-2020 Xenios SEZC
// All rights reserved.
// https://www.veriblock.org
// Distributed under the MIT software license, see the accompanying
// file LICENSE or http://www.opensource.org/licenses/mit-license.php.

package nodecore.cli.serialization;

import com.google.gson.annotations.SerializedName;
import nodecore.api.grpc.VeriBlockMessages;
import nodecore.api.grpc.utilities.ByteStringAddressUtility;

import java.util.ArrayList;
import java.util.List;

public class NewAddressPayload {
    @SerializedName("address")
    public String address;

    @SerializedName("additional_address")
    public List<String> additionalAddresses;

    public NewAddressPayload(final VeriBlockMessages.GetNewAddressReply reply) {
        address = ByteStringAddressUtility.parseProperAddressTypeAutomatically(reply.getAddress());
        if (reply.getAdditionalAddressesCount() > 0) {
            additionalAddresses = new ArrayList<>(reply.getAdditionalAddressesCount());
            reply.getAdditionalAddressesList()
                    .forEach(bytes -> additionalAddresses.add(ByteStringAddressUtility.parseProperAddressTypeAutomatically(bytes)));
        }
    }
}
