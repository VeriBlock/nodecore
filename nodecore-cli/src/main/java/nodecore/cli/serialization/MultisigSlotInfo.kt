// VeriBlock NodeCore
// Copyright 2017-2021 Xenios SEZC
// All rights reserved.
// https://www.veriblock.org
// Distributed under the MIT software license, see the accompanying
// file LICENSE or http://www.opensource.org/licenses/mit-license.php.
package nodecore.cli.serialization

import nodecore.api.grpc.RpcMultisigSlot
import org.veriblock.core.utilities.AddressUtility
import org.veriblock.core.utilities.extensions.toBase58
import org.veriblock.core.utilities.extensions.toHex

class MultisigSlotInfo(
    slot: RpcMultisigSlot
) {
    val publicKey = if (slot.publicKey != null) {
        slot.publicKey.toByteArray().toHex()
    } else {
        "-"
    }

    val signature = if (slot.publicKey != null) {
        slot.signature.toByteArray().toHex()
    } else {
        "-"
    }

    val ownerAddress = if (slot.publicKey != null) {
        AddressUtility.addressFromPublicKey(slot.publicKey.toByteArray())
    } else {
        slot.ownerAddress.toByteArray().toBase58()
    }

    val populated = slot.populated
}
