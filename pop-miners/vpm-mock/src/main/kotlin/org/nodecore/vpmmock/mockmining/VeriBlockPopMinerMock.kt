// VeriBlock Blockchain Project
// Copyright 2017-2018 VeriBlock, Inc
// Copyright 2018-2019 Xenios SEZC
// All rights reserved.
// https://www.veriblock.org
// Distributed under the MIT software license, see the accompanying
// file LICENSE or http://www.opensource.org/licenses/mit-license.php.
package org.veriblock.miners.pop.service.mockmining

import io.netty.buffer.ByteBuf
import org.slf4j.LoggerFactory
import org.veriblock.core.crypto.Sha256Hash
import org.veriblock.core.params.BitcoinRegTestParameters
import org.veriblock.core.params.NetworkParameters
import org.veriblock.core.params.defaultRegTestParameters
import org.veriblock.core.utilities.Utility
import org.veriblock.sdk.blockchain.store.BitcoinStore
import org.veriblock.sdk.models.*
import org.veriblock.sdk.services.SerializeDeserializeService
import org.veriblock.sdk.sqlite.ConnectionSelector
import java.nio.ByteBuffer
import java.security.*

fun serializePublicationData(header: ByteArray, address: Address): ByteArray {
    val buffer = ByteBuffer.allocateDirect(80)
    buffer.put(header)
    buffer.put(address.poPBytes)
    buffer.flip()
    val payoutInfo = ByteArray(80)
    buffer.get(payoutInfo)
    return payoutInfo
}

class VeriBlockPopMinerMock(
    val veriBlockParameters: NetworkParameters = defaultRegTestParameters,
    val bitcoinBlockchain: BitcoinMockBlockchain = BitcoinMockBlockchain(
        veriBlockParameters.bitcoinOriginBlock,
        veriBlockParameters.bitcoinOriginBlockHeight,
        BitcoinRegTestParameters(),
        BitcoinStore(
            ConnectionSelector.setConnectionInMemory("vpm-mock")
        )
    ),
) {
    val bitcoinMempool = ArrayList<BitcoinTransaction>()

    fun createBtcTx(publishedBlock: VeriBlockBlock, address: Address): BitcoinTransaction {
        return BitcoinTransaction(PublicationData(publishedBlock, address).serialize())
    }

    fun createBtcTx(publishedBlock: ByteArray, address: Address): BitcoinTransaction {
        return BitcoinTransaction(PublicationData(publishedBlock, address).serialize())
    }

    /**
     * Mines 'number' of blocks. If mempool is not empty, first block will contain
     * all transactions from mempool. Mempool will be cleared after this call.
     *
     * @param  number Number of blocks to mine
     */
    fun mineBtcBlocks(number: Int): BitcoinBlock? {
        assert(number.compareTo(0) > 0)

        var remaininBlocks = number
        var last: BitcoinBlock? = null
        if (!bitcoinMempool.isEmpty()) {
            // mine all txes from mempool into first block
            val blockData = BitcoinBlockData()
            blockData.addAll(bitcoinMempool.map { x -> x.rawBytes })
            bitcoinMempool.clear()
            last = bitcoinBlockchain.mine(blockData)
            --remaininBlocks
        }

        // if we need to mine more than 1 block on top, mine empty blocks
        while (remaininBlocks > 0) {
            last = bitcoinBlockchain.mine(BitcoinBlockData())
            --remaininBlocks
        }

        return last
    }

    /**
     * Creates signed VeriBlock POP transaction from Bitcoin Block data
     *
     * @param hash BTC block hash with proofs
     * @param index array index of BTC TX inside blof of proof
     * @param key Secp256k1 keypair to sign VBK POP TX
     * @param lastKnownBTCBlock used for building context between this block and block of proof
     */
    fun createVbkPopTx(
        // blockOfProof BTC block hash
        hash: Sha256Hash,
        // N of transaction in blockOfProof BTC block
        index: Int,
        // keypair to sign VbkPopTx
        key: KeyPair,
        lastKnownBTCBlock: BitcoinBlock
    ): VeriBlockPopTransaction? {
        val blockData = bitcoinBlockchain.blockDataStore[hash]
            ?: return null
        val publishedBlock = deserializeVbkHeader(ByteBuffer.wrap(blockData[index]))
        val bitcoinProofTx = BitcoinTransaction(blockData[index])
        val address = Address.fromPublicKey(key.public.encoded)

        val blockOfProof = bitcoinBlockchain.get(hash)
            ?: return null
        val blockOfProofContext = bitcoinBlockchain.getContext(lastKnownBTCBlock)
        return signTransaction(
            VeriBlockPopTransaction(
                address,
                publishedBlock,
                bitcoinProofTx,
                blockData.getMerklePath(index),
                blockOfProof,
                blockOfProofContext,
                ByteArray(1),
                key.public.encoded,
                veriBlockParameters.transactionPrefix
            ),
            key.private
        )
    }

    class PublicationData(
        val publishedBlock: VeriBlockBlock,
        val address: Address
    ) {
        fun serialize(): ByteArray = serializePublicationData(
            SerializeDeserializeService.serializeHeaders(publishedBlock), address
        )
    }

    @Throws(SignatureException::class, InvalidKeyException::class, NoSuchAlgorithmException::class)
    private fun signTransaction(tx: VeriBlockPopTransaction, privateKey: PrivateKey): VeriBlockPopTransaction {
        val signature = Utility.signMessageWithPrivateKey(
            SerializeDeserializeService.getId(tx).bytes,
            privateKey
        )
        return VeriBlockPopTransaction(
            tx.address,
            tx.publishedBlock,
            tx.bitcoinTransaction,
            tx.merklePath,
            tx.blockOfProof,
            tx.blockOfProofContext,
            signature,
            tx.publicKey,
            tx.networkByte
        )
    }

    private fun deserializeVbkHeader(bytes: ByteBuffer): VeriBlockBlock {
        return SerializeDeserializeService.parseVeriBlockBlockStream(bytes)
    }

}
