// VeriBlock NodeCore CLI
// Copyright 2017-2019 Xenios SEZC
// All rights reserved.
// https://www.veriblock.org
// Distributed under the MIT software license, see the accompanying
// file LICENSE or http://www.opensource.org/licenses/mit-license.php.

package nodecore.cli.commands.serialization;

import nodecore.api.grpc.AddressSignatureIndexes;
import nodecore.api.grpc.GetSignatureIndexReply;

import java.util.ArrayList;
import java.util.List;

public class SignatureIndexPayload {
    public SignatureIndexPayload(final GetSignatureIndexReply reply) {
        for (final AddressSignatureIndexes indexes : reply.getIndexesList())
            addresses.add(new SignatureIndexInfo(indexes));
    }

    public List<SignatureIndexInfo> addresses = new ArrayList<>();
}
