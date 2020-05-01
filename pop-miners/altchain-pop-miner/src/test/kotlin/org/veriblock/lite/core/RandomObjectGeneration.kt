// VeriBlock Blockchain Project
// Copyright 2017-2018 VeriBlock, Inc
// Copyright 2018-2020 Xenios SEZC
// All rights reserved.
// https://www.veriblock.org
// Distributed under the MIT software license, see the accompanying
// file LICENSE or http://www.opensource.org/licenses/mit-license.php.

package org.veriblock.lite.core

import org.apache.commons.lang3.RandomStringUtils
import org.veriblock.core.utilities.AddressUtility
import org.veriblock.core.wallet.AddressKeyGenerator
import org.veriblock.lite.net.NodeCoreGateway
import org.veriblock.lite.transactionmonitor.TransactionMonitor
import org.veriblock.lite.transactionmonitor.WalletTransaction
import org.veriblock.sdk.models.Address
import org.veriblock.sdk.models.BitcoinBlock
import org.veriblock.sdk.models.BitcoinTransaction
import org.veriblock.sdk.models.Coin
import org.veriblock.sdk.models.MerklePath
import org.veriblock.sdk.models.Output
import org.veriblock.sdk.models.PublicationData
import org.veriblock.sdk.models.Sha256Hash
import org.veriblock.sdk.models.VBlakeHash
import org.veriblock.sdk.models.VeriBlockBlock
import org.veriblock.sdk.models.VeriBlockMerklePath
import org.veriblock.sdk.models.VeriBlockPoPTransaction
import org.veriblock.sdk.models.VeriBlockTransaction
import java.security.MessageDigest
import kotlin.random.Random

fun randomBoolean() = Random.nextBoolean()

fun randomInt(bound: Int) = Random.nextInt(bound)

fun randomInt(min: Int, max: Int) = Random.nextInt(max - min) + min

fun randomLong(min: Long, max: Long) = (Random.nextDouble() * (max - min)).toLong() + min

fun randomAlphabeticString(length: Int = 10): String =
    RandomStringUtils.randomAlphabetic(length)

fun randomAlphabeticString(minLengthInclusive: Int = 8, maxLengthInclusive: Int = 12): String =
    RandomStringUtils.randomAlphabetic(minLengthInclusive, maxLengthInclusive)

fun randomByteArray(size: Int): ByteArray {
    return ByteArray(size) {
        randomInt(256).toByte()
    }
}

fun randomCoin(
    amount: Long = randomLong(1, 10_000_000_000)
): Coin {
    return Coin.valueOf(amount)
}

fun randomAddress(): Address {
    val pair = AddressKeyGenerator.generate()
    val address = AddressUtility.addressFromPublicKey(pair.public)
    return Address(address)
}

fun randomPublicationData() = PublicationData(
    randomLong(0, 10000),
    randomByteArray(64),
    randomByteArray(32),
    randomByteArray(32)
)

fun randomWalletTransaction(
    context: Context,
    type: Byte = 0x01,
    sourceAddress: Address = randomAddress(),
    sourceAmount: Coin = randomCoin(),
    outputs: List<Output> = (1..10).map { randomOutput() },
    signatureIndex: Long = 7,
    publicationData: PublicationData = randomPublicationData(),
    signature: ByteArray = ByteArray(10),
    publicKey: ByteArray = ByteArray(8),
    networkByte: Byte? = context.networkParameters.transactionPrefix,
    transactionMeta: TransactionMeta = randomTransactionMeta(),
    merklePath: VeriBlockMerklePath = randomVeriBlockMerklePath()
): WalletTransaction {
    return WalletTransaction(
        type,
        sourceAddress,
        sourceAmount,
        outputs,
        signatureIndex,
        publicationData,
        signature,
        publicKey,
        networkByte,
        transactionMeta
    ).apply {
        this.merklePath = merklePath
    }
}

fun randomOutput(
    address: Address = randomAddress(),
    amount: Coin = randomCoin()
): Output {
    return Output(address, amount)
}

fun randomTransactionMeta(
    transactionId: Sha256Hash = randomSha256Hash(),
    metaState: TransactionMeta.MetaState = TransactionMeta.MetaState.UNKNOWN,
    depthCount: Int = 0
): TransactionMeta {
    return TransactionMeta(transactionId).apply {
        appearsInBestChainBlock = randomVBlakeHash()
        setState(metaState)
        depth = depthCount
    }
}

fun randomVBlakeHash(): VBlakeHash {
    return VBlakeHash.wrap(randomByteArray(VBlakeHash.VERIBLOCK_LENGTH))
}

private var messageDigest = MessageDigest.getInstance("SHA-256")
fun randomSha256Hash(): Sha256Hash {
    val randomBytes = randomAlphabeticString().toByteArray()
    return Sha256Hash.wrap(messageDigest.digest(randomBytes))
}

fun randomTransactionMonitor(
    context: Context,
    gateway: NodeCoreGateway,
    address: Address = randomAddress(),
    walletTransactions: List<WalletTransaction> = (0..randomInt(20)).map { randomWalletTransaction(context) }
) = TransactionMonitor(context, address, gateway, walletTransactions)

fun randomVeriBlockMerklePath(
    treeIndex: Int = randomInt(1, 65535),
    index: Int = randomInt(1, 65535),
    subject: Sha256Hash = randomSha256Hash(),
    layers: List<Sha256Hash> = (1..5).map { randomSha256Hash() }
): VeriBlockMerklePath {
    return VeriBlockMerklePath(
        "$treeIndex:$index:$subject:${layers.joinToString(separator = ":") { it.toString() }}"
    )
}

fun randomMerklePath(
    index: Int = randomInt(1, 65535),
    subject: Sha256Hash = randomSha256Hash(),
    layers: List<Sha256Hash> = (1..5).map { randomSha256Hash() }
): MerklePath {
    return MerklePath(
        "$index:$subject:${layers.joinToString(separator = ":") { it.toString() }}"
    )
}

fun randomVeriBlockTransaction(
    context: Context,
    type: Byte = 0x01,
    sourceAddress: Address = randomAddress(),
    sourceAmount: Coin = randomCoin(),
    outputs: List<Output> = (1..10).map { randomOutput() },
    signatureIndex: Long = 7,
    publicationData: PublicationData = randomPublicationData(),
    signature: ByteArray = ByteArray(10),
    publicKey: ByteArray = ByteArray(8),
    networkByte: Byte? = context.networkParameters.transactionPrefix
): VeriBlockTransaction {
    return VeriBlockTransaction(
        type,
        sourceAddress,
        sourceAmount,
        outputs,
        signatureIndex,
        publicationData,
        signature,
        publicKey,
        networkByte
    )
}

fun randomFullBlock(
    context: Context,
    height: Int = randomInt(0, Int.MAX_VALUE),
    version: Short = randomInt(0, Short.MAX_VALUE.toInt()).toShort(),
    previousBlock: VBlakeHash = randomVBlakeHash(),
    previousKeystone: VBlakeHash = randomVBlakeHash(),
    secondPreviousKeystone: VBlakeHash = randomVBlakeHash(),
    merkleRoot: Sha256Hash = randomSha256Hash(),
    timestamp: Int = randomInt(0, Int.MAX_VALUE),
    difficulty: Int = randomInt(0, Int.MAX_VALUE),
    nonce: Int = randomInt(0, Int.MAX_VALUE),
    normalTransactions: List<VeriBlockTransaction> = (1..10).map { randomVeriBlockTransaction(context) },
    poPTransactions: List<VeriBlockPoPTransaction> = (1..10).map { randomVeriBlockPoPTransaction(context) },
    metaPackage: BlockMetaPackage = randomBlockMetaPackage()
): FullBlock {
    return FullBlock(
        height,
        version,
        previousBlock,
        previousKeystone,
        secondPreviousKeystone,
        merkleRoot,
        timestamp,
        difficulty,
        nonce,
        normalTransactions,
        poPTransactions,
        metaPackage
    )
}

fun randomBlockMetaPackage(
    hash: Sha256Hash = randomSha256Hash()
): BlockMetaPackage {
    return BlockMetaPackage(hash)
}

fun randomVeriBlockPoPTransaction(
    context: Context,
    address: Address = randomAddress(),
    publishedBlock: VeriBlockBlock = randomVeriBlockBlock(),
    bitcoinTransaction: BitcoinTransaction = randomBitcoinTransaction(),
    merklePath: MerklePath = randomMerklePath(),
    blockOfProof: BitcoinBlock = randomBitcoinBlock(),
    blockOfProofContext: List<BitcoinBlock> = (1..10).map { randomBitcoinBlock() },
    signature: ByteArray = ByteArray(10),
    publicKey: ByteArray = ByteArray(8),
    networkByte: Byte? = context.networkParameters.transactionPrefix
): VeriBlockPoPTransaction {
    return VeriBlockPoPTransaction(
        address,
        publishedBlock,
        bitcoinTransaction,
        merklePath,
        blockOfProof,
        blockOfProofContext,
        signature,
        publicKey,
        networkByte
    )
}

fun randomVeriBlockBlock(
    height: Int = randomInt(1, 65535),
    version: Short = randomInt(1, Short.MAX_VALUE.toInt()).toShort(),
    previousBlock: VBlakeHash = randomVBlakeHash(),
    previousKeystone: VBlakeHash = randomVBlakeHash(),
    secondPreviousKeystone: VBlakeHash = randomVBlakeHash(),
    merkleRoot: Sha256Hash = randomSha256Hash(),
    timestamp: Int = randomInt(1, 65535),
    difficulty: Int = randomInt(1, 65535),
    nonce: Int = randomInt(1, 35535)
): VeriBlockBlock {
    return VeriBlockBlock(
        height,
        version,
        previousBlock,
        previousKeystone,
        secondPreviousKeystone,
        merkleRoot,
        timestamp,
        difficulty,
        nonce
    )
}

fun randomBitcoinTransaction(
    raw: ByteArray = randomByteArray(243)
): BitcoinTransaction {
    return BitcoinTransaction(raw)
}

fun randomBitcoinBlock(
    version: Int = randomInt(1, 65535),
    previousBlock: Sha256Hash = randomSha256Hash(),
    merkleRoot: Sha256Hash = randomSha256Hash(),
    timestamp: Int = randomInt(1, 65535),
    bits: Int = randomInt(1, 65535),
    nonce: Int = randomInt(1, 65535)
): BitcoinBlock {
    return BitcoinBlock(
        version,
        previousBlock,
        merkleRoot,
        timestamp,
        bits,
        nonce
    )
}
