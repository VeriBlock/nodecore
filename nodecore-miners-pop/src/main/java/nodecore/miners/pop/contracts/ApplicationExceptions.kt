// VeriBlock PoP Miner
// Copyright 2017-2019 Xenios SEZC
// All rights reserved.
// https://www.veriblock.org
// Distributed under the MIT software license, see the accompanying
// file LICENSE or http://www.opensource.org/licenses/mit-license.php.
package nodecore.miners.pop.contracts

class ApplicationExceptions {
    class ExceededMaxTransactionFee : RuntimeException()
    class PoPSubmitRejected : RuntimeException()
    class CorruptSPVChain(message: String?) : RuntimeException(message)
    class UnableToAcquireTransactionLock : RuntimeException()
    class DuplicateTransactionException : RuntimeException()
    class SendTransactionException(inner: Exception?) : RuntimeException() {
        init {
            this.addSuppressed(inner)
        }
    }
}
