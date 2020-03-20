// VeriBlock NodeCore
// Copyright 2017-2020 Xenios SEZC
// All rights reserved.
// https://www.veriblock.org
// Distributed under the MIT software license, see the accompanying
// file LICENSE or http://www.opensource.org/licenses/mit-license.php.

package nodecore.cli.serialization;

import nodecore.api.grpc.VeriBlockMessages;
import org.veriblock.core.bitcoinj.Base58;
import org.veriblock.core.utilities.AddressUtility;
import org.veriblock.core.utilities.Utility;

public class MultisigSlotInfo {
    public MultisigSlotInfo(VeriBlockMessages.MultisigSlot slot) {
        populated = slot.getPopulated();
        if (slot.getPublicKey() != null) {
            publicKey = Utility.bytesToHex(slot.getPublicKey().toByteArray());
            signature = Utility.bytesToHex(slot.getSignature().toByteArray());
            ownerAddress = AddressUtility.addressFromPublicKey(slot.getPublicKey().toByteArray());
        } else {
            publicKey = "-";
            signature = "-";
            ownerAddress = Base58.encode(slot.getOwnerAddress().toByteArray());
        }
    }

    public String publicKey;
    public String signature;
    public String ownerAddress;
    public boolean populated;
}
