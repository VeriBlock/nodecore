// VeriBlock Blockchain Project
// Copyright 2017-2018 VeriBlock, Inc
// Copyright 2018-2020 Xenios SEZC
// All rights reserved.
// https://www.veriblock.org
// Distributed under the MIT software license, see the accompanying
// file LICENSE or http://www.opensource.org/licenses/mit-license.php.
package veriblock.model

enum class TransactionTypeIdentifier(
    val id: Byte
) {
    STANDARD(0x01.toByte()),
    PROOF_OF_PROOF(0x02.toByte()),
    MULTISIG(0x03.toByte());
}
