// VeriBlock Blockchain Project
// Copyright 2017-2018 VeriBlock, Inc
// Copyright 2018-2020 Xenios SEZC
// All rights reserved.
// https://www.veriblock.org
// Distributed under the MIT software license, see the accompanying
// file LICENSE or http://www.opensource.org/licenses/mit-license.php.

package org.veriblock.lite.net

import nodecore.api.grpc.VeriBlockMessages
import nodecore.api.grpc.VeriBlockMessages.GetTransactionsReply
import nodecore.api.grpc.VeriBlockMessages.GetTransactionsRequest

interface GatewayStrategy {

    fun getBalance(getBalanceRequest: VeriBlockMessages.GetBalanceRequest): VeriBlockMessages.GetBalanceReply

    fun getVeriBlockPublications(getVeriBlockPublicationsRequest: VeriBlockMessages.GetVeriBlockPublicationsRequest): VeriBlockMessages.GetVeriBlockPublicationsReply

    fun getDebugVeriBlockPublications(getDebugVTBsRequest: VeriBlockMessages.GetDebugVTBsRequest): VeriBlockMessages.GetDebugVTBsReply

    fun ping(pingRequest: VeriBlockMessages.PingRequest): VeriBlockMessages.PingReply

    fun getNodeCoreSyncStatus(getStateInfoRequest: VeriBlockMessages.GetStateInfoRequest): VeriBlockMessages.GetStateInfoReply

    fun submitTransactions(submitTransactionsRequest: VeriBlockMessages.SubmitTransactionsRequest): VeriBlockMessages.ProtocolReply

    fun createAltChainEndorsement(altChainEndorsementRequest: VeriBlockMessages.CreateAltChainEndorsementRequest): VeriBlockMessages.CreateAltChainEndorsementReply

    fun getLastVBKBlockHeader(): VeriBlockMessages.BlockHeader

    fun getVBKBlockHeader(blockHash: ByteArray): VeriBlockMessages.BlockHeader

    fun getVBKBlockHeader(blockHeight: Int): VeriBlockMessages.BlockHeader

    fun getTransactions(request: GetTransactionsRequest): GetTransactionsReply

    fun shutdown()
}
