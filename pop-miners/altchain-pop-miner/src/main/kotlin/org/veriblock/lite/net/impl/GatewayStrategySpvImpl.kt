// VeriBlock Blockchain Project
// Copyright 2017-2018 VeriBlock, Inc
// Copyright 2018-2020 Xenios SEZC
// All rights reserved.
// https://www.veriblock.org
// Distributed under the MIT software license, see the accompanying
// file LICENSE or http://www.opensource.org/licenses/mit-license.php.

package org.veriblock.lite.net.impl

import com.google.protobuf.ByteString
import nodecore.api.grpc.VeriBlockMessages
import org.veriblock.lite.net.GatewayStrategy
import veriblock.SpvContext
import veriblock.service.AdminApiService

class GatewayStrategySpvImpl(
    private val spvContext: SpvContext
) : GatewayStrategy {

    private val adminApiService: AdminApiService = spvContext.adminApiService

    override fun getBalance(getBalanceRequest: VeriBlockMessages.GetBalanceRequest): VeriBlockMessages.GetBalanceReply {
        return adminApiService.getBalance(getBalanceRequest)
    }

    override fun getNodeCoreSyncStatus(getStateInfoRequest: VeriBlockMessages.GetStateInfoRequest): VeriBlockMessages.GetStateInfoReply {
        return adminApiService.getStateInfo(getStateInfoRequest)
    }

    override fun submitTransactions(submitTransactionsRequest: VeriBlockMessages.SubmitTransactionsRequest): VeriBlockMessages.ProtocolReply {
        return adminApiService.submitTransactions(submitTransactionsRequest)
    }

    override fun createAltChainEndorsement(altChainEndorsementRequest: VeriBlockMessages.CreateAltChainEndorsementRequest): VeriBlockMessages.CreateAltChainEndorsementReply {
        return adminApiService.createAltChainEndorsement(altChainEndorsementRequest)
    }

    override fun getLastVBKBlockHeader(): VeriBlockMessages.BlockHeader {
        return adminApiService.lastVBKBlockHeader
    }

    override fun getVBKBlockHeader(blockHash: ByteArray): VeriBlockMessages.BlockHeader {
        return adminApiService.getVbkBlockHeader(ByteString.copyFrom(blockHash))
    }

    override fun getVBKBlockHeader(blockHeight: Int): VeriBlockMessages.BlockHeader {
        return adminApiService.getVbkBlockHeader(blockHeight)
    }

    override fun ping(pingRequest: VeriBlockMessages.PingRequest): VeriBlockMessages.PingReply {
        return VeriBlockMessages.PingReply.getDefaultInstance()
    }

    override fun getVeriBlockPublications(getVeriBlockPublicationsRequest: VeriBlockMessages.GetVeriBlockPublicationsRequest): VeriBlockMessages.GetVeriBlockPublicationsReply {
        return adminApiService.getVeriBlockPublications(getVeriBlockPublicationsRequest)
    }

    override fun getTransactions(request: VeriBlockMessages.GetTransactionsRequest): VeriBlockMessages.GetTransactionsReply {
        return adminApiService.getTransactions(request)
    }

    override fun shutdown() {
        spvContext.peerTable.shutdown()
    }

    override fun getDebugVeriBlockPublications(getDebugVTBsRequest: VeriBlockMessages.GetDebugVTBsRequest): VeriBlockMessages.GetDebugVTBsReply {
        TODO("Not yet implemented")
    }

}
