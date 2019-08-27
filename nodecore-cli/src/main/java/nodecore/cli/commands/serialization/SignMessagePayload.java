// VeriBlock NodeCore
// Copyright 2017-2019 Xenios SEZC
// All rights reserved.
// https://www.veriblock.org
// Distributed under the MIT software license, see the accompanying
// file LICENSE or http://www.opensource.org/licenses/mit-license.php.

package nodecore.cli.commands.serialization;

import nodecore.api.grpc.SignMessageReply;
import nodecore.api.grpc.utilities.ByteStringUtility;

public class SignMessagePayload {
    public String address;
    public String signedMessage;
    public String publicKey;
    public SignMessagePayload(String address, SignMessageReply message) {
        this.address = address;
        this.signedMessage = ByteStringUtility.byteStringToHex(message.getSignedMessage());
        this.publicKey = ByteStringUtility.byteStringToHex(message.getPublicKey());
    }
}
