// VeriBlock Blockchain Project
// Copyright 2017-2018 VeriBlock, Inc
// Copyright 2018-2020 Xenios SEZC
// All rights reserved.
// https://www.veriblock.org
// Distributed under the MIT software license, see the accompanying
// file LICENSE or http://www.opensource.org/licenses/mit-license.php.

package org.veriblock.lite.net

import nodecore.api.grpc.VeriBlockMessages

interface GatewayStrategy {

    fun getBalance(getBalanceRequest: VeriBlockMessages.GetBalanceRequest): VeriBlockMessages.GetBalanceReply

    fun getVeriBlockPublications(getVeriBlockPublicationsRequest: VeriBlockMessages.GetVeriBlockPublicationsRequest): VeriBlockMessages.GetVeriBlockPublicationsReply

    fun getDebugVeriBlockPublications(getDebugVTBsRequest: VeriBlockMessages.GetDebugVTBsRequest): VeriBlockMessages.GetDebugVTBsReply

    fun ping(pingRequest: VeriBlockMessages.PingRequest): VeriBlockMessages.PingReply

    fun getNodeCoreSyncStatus(getStateInfoRequest: VeriBlockMessages.GetStateInfoRequest): VeriBlockMessages.GetStateInfoReply

    fun submitTransactions(submitTransactionsRequest: VeriBlockMessages.SubmitTransactionsRequest): VeriBlockMessages.ProtocolReply

    fun createAltChainEndorsement(altChainEndorsementRequest: VeriBlockMessages.CreateAltChainEndorsementRequest): VeriBlockMessages.CreateAltChainEndorsementReply

    fun shutdown()

    //TODO remove on the next step
    fun listChangesSince(listBlocksSinceRequest: VeriBlockMessages.ListBlocksSinceRequest): VeriBlockMessages.ListBlocksSinceReply
    fun getBlock(getBlocksRequest: VeriBlockMessages.GetBlocksRequest): VeriBlockMessages.GetBlocksReply
    fun getLastBlock(getLastBlockRequest: VeriBlockMessages.GetLastBlockRequest): VeriBlockMessages.GetLastBlockReply
}
