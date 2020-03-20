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

public class ImportPrivateKeyInfo {
    public ImportPrivateKeyInfo(final VeriBlockMessages.ImportPrivateKeyReply reply) {
        this.address = ByteStringAddressUtility.parseProperAddressTypeAutomatically(reply.getResultantAddress());
    }

    @SerializedName("imported_address")
    public String address;
}
