// VeriBlock Blockchain Project
// Copyright 2017-2018 VeriBlock, Inc
// Copyright 2018-2020 Xenios SEZC
// All rights reserved.
// https://www.veriblock.org
// Distributed under the MIT software license, see the accompanying
// file LICENSE or http://www.opensource.org/licenses/mit-license.php.
package org.veriblock.spv.model

import org.veriblock.core.crypto.Sha256Hash
import org.veriblock.core.crypto.VbkTxId

class MultisigTransaction(txId: VbkTxId) : StandardTransaction(txId) {
    override val transactionTypeIdentifier: TransactionTypeIdentifier
        get() = TransactionTypeIdentifier.MULTISIG
}
