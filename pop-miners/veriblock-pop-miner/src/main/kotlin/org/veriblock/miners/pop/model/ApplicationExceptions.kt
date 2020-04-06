// VeriBlock PoP Miner
// Copyright 2017-2020 Xenios SEZC
// All rights reserved.
// https://www.veriblock.org
// Distributed under the MIT software license, see the accompanying
// file LICENSE or http://www.opensource.org/licenses/mit-license.php.
package org.veriblock.miners.pop.model

class ApplicationExceptions {
    class ExceededMaxTransactionFee : RuntimeException("Calculated fee exceeded configured maximum transaction fee")
    class CorruptSPVChain(message: String) : RuntimeException(message)
    class UnableToAcquireTransactionLock : RuntimeException() {
        override val message: String = "A previous transaction has not yet completed broadcasting to peers and new transactions would result in double spending. Wait a few seconds and try again."
    }

    class DuplicateTransactionException : RuntimeException() {
        override val message: String = "Transaction appears identical to a previously broadcast transaction. Often this occurs when there is a 'too-long-mempool-chain'."
    }
}
