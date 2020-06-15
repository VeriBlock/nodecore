// VeriBlock Blockchain Project
// Copyright 2017-2018 VeriBlock, Inc
// Copyright 2018-2019 Xenios SEZC
// All rights reserved.
// https://www.veriblock.org
// Distributed under the MIT software license, see the accompanying
// file LICENSE or http://www.opensource.org/licenses/mit-license.php.
package org.veriblock.sdk.models

enum class BlockType(
    var id: Byte
) {
    VERIBLOCK_TX(0x01.toByte()),
    VERIBLOCK_POP_TX(0x02.toByte());
}
