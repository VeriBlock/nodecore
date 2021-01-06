// VeriBlock NodeCore
// Copyright 2017-2020 Xenios SEZC
// All rights reserved.
// https://www.veriblock.org
// Distributed under the MIT software license, see the accompanying
// file LICENSE or http://www.opensource.org/licenses/mit-license.php.
package nodecore.cli.serialization

import nodecore.api.grpc.VeriBlockMessages.MultisigSlot
import org.veriblock.core.bitcoinj.Base58
import org.veriblock.core.utilities.AddressUtility
import org.veriblock.core.utilities.Utility

class MultisigSlotInfo(
    slot: MultisigSlot
) {
    val publicKey = if (slot.publicKey != null) {
        Utility.bytesToHex(slot.publicKey.toByteArray())
    } else {
        "-"
    }

    val signature = if (slot.publicKey != null) {
        Utility.bytesToHex(slot.signature.toByteArray())
    } else {
        "-"
    }

    val ownerAddress = if (slot.publicKey != null) {
        AddressUtility.addressFromPublicKey(slot.publicKey.toByteArray())
    } else {
        Base58.encode(slot.ownerAddress.toByteArray())
    }

    val populated = slot.populated
}
