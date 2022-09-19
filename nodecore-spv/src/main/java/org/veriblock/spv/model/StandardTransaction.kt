// VeriBlock Blockchain Project
// Copyright 2017-2018 VeriBlock, Inc
// Copyright 2018-2021 Xenios SEZC
// All rights reserved.
// https://www.veriblock.org
// Distributed under the MIT software license, see the accompanying
// file LICENSE or http://www.opensource.org/licenses/mit-license.php.
package org.veriblock.spv.model

import com.google.protobuf.ByteString
import nodecore.api.grpc.RpcSignedTransaction
import nodecore.api.grpc.RpcTransaction
import nodecore.api.grpc.utilities.extensions.asHexByteString
import org.veriblock.core.crypto.Crypto
import org.veriblock.core.utilities.SerializerUtility
import org.veriblock.core.utilities.Utility
import org.veriblock.core.utilities.createLogger
import org.veriblock.sdk.models.Coin
import org.veriblock.core.crypto.Sha256Hash
import org.veriblock.core.crypto.VbkTxId
import org.veriblock.core.crypto.asVbkTxId
import org.veriblock.core.params.NetworkParameters
import org.veriblock.sdk.models.VeriBlockTransaction
import org.veriblock.sdk.models.asCoin
import org.veriblock.sdk.services.SerializeDeserializeService
import java.io.ByteArrayOutputStream
import java.io.IOException
import java.io.OutputStream
import java.util.ArrayList

private val logger = createLogger {}

class StandardTransaction(
    val tx: VeriBlockTransaction,
    val meta: TransactionMeta
)
