// VeriBlock Blockchain Project
// Copyright 2017-2018 VeriBlock, Inc
// Copyright 2018-2020 Xenios SEZC
// All rights reserved.
// https://www.veriblock.org
// Distributed under the MIT software license, see the accompanying
// file LICENSE or http://www.opensource.org/licenses/mit-license.php.
package veriblock.model

import org.veriblock.sdk.models.Sha256Hash

class MultisigTransaction(txId: Sha256Hash) : StandardTransaction(txId) {
    override val transactionTypeIdentifier: TransactionTypeIdentifier
        get() = TransactionTypeIdentifier.MULTISIG
}
