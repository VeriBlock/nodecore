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

internal data class EthPopData(
    val state: EthPopStateData
)

internal data class EthPopStateData(
    val endorsedBy: List<String>,
    val stored: EthPopStoredStateData
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

internal data class BlockChainInfo(
    val chain: String,
    val blocks: Int,
    val headers: Int,
    val initialblockdownload: Boolean
)

internal data class EthBlockBlock(
    val chainWork: String?,
    val height: Int?,
    val header: EthBlockHeader?,
    val status: Int?,
    val vbkRefs: List<Int?>?,
    val blockOfProofEndorsements: List<String?>?
)

internal data class EthBlockHeader(
    val hash: String?,
    val version: Int?,
    val previousBlock: String?,
    val merkleRoot: String?,
    val timestamp: Int?,
    val bits: Long?,
    val nonce: Int?
)

internal data class VbkBlock(
    val chainWork: String?,
    val containingEndorsements: List<String?>?,
    val endorsedBy: List<Int?>?,
    val height: Int?,
    val header: VbkBlockHeader?,
    val status: Int?,
    val ref: Int?,
    val stored: StoredVtbIds?,
    val blockOfProofEndorsements: List<String?>?
)

internal data class VbkBlockHeader(
    val id: String?,
    val hash: String?,
    val height: Int?,
    val version: Short?,
    val previousBlock: String?,
    val previousKeystone: String?,
    val secondPreviousKeystone: String?,
    val merkleRoot: String?,
    val timestamp: Int?,
    val difficulty: Int?,
    val nonce: Long?,
    val stored: StoredVtbIds?,
    val blockOfProofEndorsements: List<String?>?
)

internal data class StoredVtbIds(
    val vtbids: List<String?>?
)

internal data class ValidationData(
    val id: String,
    val validity: ValidityInfo
)

internal data class ValidityInfo(
    val state: String,
    val code: String,
    val message: String
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

fun String.asEthHexInt() = drop(2).toInt(16)
fun String.asEthHexLong(): Long = drop(2).toLong(16)
fun Int.asEthHex(): String = "0x${Integer.toHexString(this)}"
