// VeriBlock NodeCore
// Copyright 2017-2020 Xenios SEZC
// All rights reserved.
// https://www.veriblock.org
// Distributed under the MIT software license, see the accompanying
// file LICENSE or http://www.opensource.org/licenses/mit-license.php.

package nodecore.cli.serialization;

import nodecore.api.grpc.VeriBlockMessages;

public class MultisigBundleInfo {
    public MultisigBundleInfo(VeriBlockMessages.MultisigBundle multisigBundle) {
        multisigSlots = new MultisigSlotInfo[multisigBundle.getSlotsCount()];
        for (int i = 0; i < multisigBundle.getSlotsCount(); i++) {
            multisigSlots[i] = new MultisigSlotInfo(multisigBundle.getSlots(i));
        }
    }
    MultisigSlotInfo[] multisigSlots;
}
