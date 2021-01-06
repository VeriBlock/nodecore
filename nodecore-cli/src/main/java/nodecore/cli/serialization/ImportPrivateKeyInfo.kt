// VeriBlock NodeCore CLI
// Copyright 2017-2020 Xenios SEZC
// All rights reserved.
// https://www.veriblock.org
// Distributed under the MIT software license, see the accompanying
// file LICENSE or http://www.opensource.org/licenses/mit-license.php.
package nodecore.cli.serialization

import com.google.gson.annotations.SerializedName
import nodecore.api.grpc.VeriBlockMessages.ImportPrivateKeyReply
import nodecore.api.grpc.utilities.ByteStringAddressUtility

class ImportPrivateKeyInfo(
    reply: ImportPrivateKeyReply
) {
    @SerializedName("imported_address")
    val address = ByteStringAddressUtility.parseProperAddressTypeAutomatically(reply.resultantAddress)
}
