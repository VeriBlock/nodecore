// VeriBlock NodeCore CLI
// Copyright 2017-2019 Xenios SEZC
// All rights reserved.
// https://www.veriblock.org
// Distributed under the MIT software license, see the accompanying
// file LICENSE or http://www.opensource.org/licenses/mit-license.php.

package nodecore.cli.commands.serialization;

import nodecore.api.grpc.GenerateMultisigAddressReply;
import nodecore.api.grpc.utilities.ByteStringAddressUtility;

public class GenerateMultisigAddressPayload {
    public GenerateMultisigAddressPayload(final GenerateMultisigAddressReply reply) {
        sourceAddresses = new String[reply.getSourceAddressesCount()];
        for (int i = 0; i < reply.getSourceAddressesCount(); i++) {
            sourceAddresses[i] = ByteStringAddressUtility.parseProperAddressTypeAutomatically(reply.getSourceAddresses(i));
        }

        signatureThresholdM = reply.getSignatureThresholdM();
        resultantMultisigAddress = ByteStringAddressUtility.parseProperAddressTypeAutomatically(reply.getMultisigAddress());
    }

    public String[] sourceAddresses;
    public int signatureThresholdM;
    public String resultantMultisigAddress;
}
