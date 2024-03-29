// VeriBlock Blockchain Project
// Copyright 2017-2018 VeriBlock, Inc
// Copyright 2018-2021 Xenios SEZC
// All rights reserved.
// https://www.veriblock.org
// Distributed under the MIT software license, see the accompanying
// file LICENSE or http://www.opensource.org/licenses/mit-license.php.

package org.veriblock.alt.plugins.bitcoin

import org.veriblock.sdk.alt.model.VtbBitcoinBlock

internal data class BtcContextInfo(
    val serialized: String
)

internal data class BtcPublicationData(
    val block_header: String,
    val authenticated_context: BtcContextInfo,
    val last_known_veriblock_blocks: List<String>,
    val last_known_bitcoin_blocks: List<String>,
)

internal data class BtcBlock(
    val hash: String,
    val height: Int,
    val confirmations: Int,
    val version: Short,
    val nonce: Long,
    val merkleroot: String,
    val difficulty: Double,
    val tx: List<String>,
    val previousblockhash: String?,
    val pop: BtcPopData
)

internal data class BtcTransaction(
    val txid: String,
    val confirmations: Int,
    val vout: List<BtcTransactionVout>,
    val blockhash: String?
)

internal data class BtcTransactionVout(
    val value: Double,
    val scriptPubKey: BtcScriptPubKey
)

internal data class BtcScriptPubKey(
    val asm: String,
    val hex: String,
    val reqSigs: Int,
    val type: String
)

internal data class BtcPopData(
    val state: BtcPopStateData
)

internal data class BtcPopStateData(
    val endorsedBy: List<String>,
    val stored: BtcPopStoredStateData
)

internal data class BtcPopStoredStateData(
    val vbkblocks: List<String>,
    val atvs: List<String>,
    val vtbs: List<String>
)

internal data class BtcAtv(
    val in_active_chain: Boolean,
    val blockheight: Int,
    val blockhash: String,
    val confirmations: Int,
    val atv: BtcAtvData
)

internal data class BtcAtvData(
    val id: String,
    val version: Int,
    val transaction: BtcVbkTransaction,
    val blockOfProof: BtcVbkBlock,
)

internal data class BtcVbkTransaction(
    val hash: String
)

internal data class BtcVbkBlock(
    val hash: String,
    val height: Int
)

internal data class BtcVtb(
    val blockhash: String,
    val vtb: Vtb
)

internal data class Vtb(
    val transaction: BtcVeriBlockPopTransaction,
    //val merklePath: VeriBlockMerklePath,
    //val containingBlock: VeriBlockBlock,
    //val context: List<VeriBlockBlock> = emptyList()
)

internal data class BtcVeriBlockPopTransaction(
    //val address: Address,
    //val publishedBlock: VeriBlockBlock,
    //val bitcoinTransaction: BitcoinTransaction,
    //val merklePath: MerklePath,
    val blockOfProof: VtbBitcoinBlock,
    val blockOfProofContext: List<VtbBitcoinBlock>,
    //val signature: ByteArray,
    //val publicKey: ByteArray,
    //val networkByte: Byte?
)

internal data class BlockChainInfo(
    val chain: String,
    val blocks: Int,
    val headers: Int,
    val initialblockdownload: Boolean
)

internal data class BtcBlockBlock(
    val chainWork: String?,
    val height: Int?,
    val header: BtcBlockHeader?,
    val status: Int?,
    val vbkRefs: List<Int?>?,
    val blockOfProofEndorsements: List<String?>?
)

internal data class BtcBlockHeader(
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
    val endorsedBy: List<String?>?,
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

data class BtcBlockEvidence(
    val height: Int,
    val hash: String,
    val previousHash: String,
    val previousKeystone: String,
    val secondPreviousKeystone: String? = null
)
