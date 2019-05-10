// VeriBlock PoP Miner
// Copyright 2017-2019 Xenios SEZC
// All rights reserved.
// https://www.veriblock.org
// Distributed under the MIT software license, see the accompanying
// file LICENSE or http://www.opensource.org/licenses/mit-license.php.

package nodecore.miners.pop.contracts;

public class ApplicationExceptions {
    public static class ExceededMaxTransactionFee extends RuntimeException {}

    public static class PoPSubmitRejected extends RuntimeException {}

    public static class CorruptSPVChain extends RuntimeException {
        public CorruptSPVChain(String message) {
            super(message);
        }
    }

    public static class UnableToAcquireTransactionLock extends RuntimeException {}

    public static class DuplicateTransactionException extends RuntimeException {}

    public static class SendTransactionException extends RuntimeException {
        public SendTransactionException (Exception inner) {
            this.addSuppressed(inner);
        }
    }
}
