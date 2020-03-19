// VeriBlock PoP Miner
// Copyright 2017-2019 Xenios SEZC
// All rights reserved.
// https://www.veriblock.org
// Distributed under the MIT software license, see the accompanying
// file LICENSE or http://www.opensource.org/licenses/mit-license.php.
package nodecore.miners.pop.model

class ApplicationExceptions {
    class ExceededMaxTransactionFee : RuntimeException("Max transaction fee exceeded")
    class PoPSubmitRejected : RuntimeException("PoP submission rejected")
    class CorruptSPVChain(message: String) : RuntimeException(message)
    class UnableToAcquireTransactionLock : RuntimeException("Unable to acquire transaction lock")
    class DuplicateTransactionException : RuntimeException("Duplicate transaction detected")
    class SendTransactionException(inner: Exception) : RuntimeException(inner.message, inner) {
        init {
            this.addSuppressed(inner)
        }
    }
}
