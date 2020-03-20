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

public class SignatureIndexInfo {
    public SignatureIndexInfo(final VeriBlockMessages.AddressSignatureIndexes indexes) {
        poolIndex = indexes.getPoolIndex();
        blockchainIndex = indexes.getBlockchainIndex();
        address = ByteStringAddressUtility.parseProperAddressTypeAutomatically(indexes.getAddress());
    }

    @SerializedName("pool_index")
    public long poolIndex;

    public String address;

    @SerializedName("blockchain_index")
    public long blockchainIndex;

}
