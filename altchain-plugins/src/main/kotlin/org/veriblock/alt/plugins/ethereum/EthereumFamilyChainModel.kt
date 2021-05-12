// VeriBlock Blockchain Project
// Copyright 2017-2018 VeriBlock, Inc
// Copyright 2018-2021 Xenios SEZC
// All rights reserved.
// https://www.veriblock.org
// Distributed under the MIT software license, see the accompanying
// file LICENSE or http://www.opensource.org/licenses/mit-license.php.

package org.veriblock.alt.plugins.ethereum

internal data class EthPublicationData(
    val block_header: String,
    val authenticated_context: String,
    val last_known_veriblock_block: String,
    val last_known_bitcoin_block: String,
)

internal data class EthBlock(
    val hash: String,
    val number: String,
    val nonce: String,
    val transactionsRoot: String,
    val difficulty: String,
    val transactions: List<String>,
    val parentHash: String?,
    val miner: String
)

internal data class EthPopBlockData(
    val pop: EthPop
)

internal class EthPop(
    val data: EthPopData,
    val state: EthPopState
)

internal class EthPopData(
    val atvs: List<Atv>,
    val vtbs: List<String>,
    val context: List<Vbk>
)

internal class EthPopState(
    val endorsedBy: List<String>
)

internal class Atv(
    val id: String
)

internal class Vbk(
    val id: String
)

internal data class EthTransaction(
    val txid: String,
    val confirmations: Int,
    val vout: List<EthTransactionVout>,
    val blockhash: String?
)

internal data class EthTransactionVout(
    val value: Double,
    val scriptPubKey: EthScriptPubKey
)

internal data class EthScriptPubKey(
    val asm: String,
    val hex: String,
    val reqSigs: Int,
    val type: String
)

internal data class EthPopStoredStateData(
    val vbkblocks: List<String>,
    val atvs: List<String>,
    val vtbs: List<String>
)

internal data class EthAtv(
    val in_active_chain: Boolean,
    val blockheight: Int,
    val blockhash: String,
    val confirmations: Int,
    val atv: EthAtvData
)

internal data class EthAtvData(
    val id: String,
    val version: Int,
    val transaction: EthVbkTransaction,
    val blockOfProof: EthVbkBlock,
)

internal data class EthVbkTransaction(
    val hash: String
)

internal data class EthVbkBlock(
    val hash: String,
    val height: Int
)

internal data class EthVtb(
    val in_active_chain: Boolean,
    val blockheight: Int,
    val confirmations: Int,
    val vtb: EthVtbData
)

internal data class EthVtbData(
    val id: String,
    val version: Int,
    val containingBlock: SpBtcBlock
)

internal data class SpBtcBlock(
    val hash: String,
    val height: Int
)

internal data class AddressValidationResponse(
    val isvalid: Boolean,
    val address: String?,
    val scriptPubKey: String?,
    val isscript: Boolean?,
    val iswitness: Boolean?,
    val witness_version: String?,
    val witness_program: String?
)

internal class EthPoPParams(
    val popActivationHeight: Long,
    val networkId: Long,
    val popPayoutDelay: Int,
    val vbkBootstrap: Int
)

