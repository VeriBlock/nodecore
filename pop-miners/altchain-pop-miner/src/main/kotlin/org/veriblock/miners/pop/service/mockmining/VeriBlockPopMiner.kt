// VeriBlock Blockchain Project
// Copyright 2017-2018 VeriBlock, Inc
// Copyright 2018-2019 Xenios SEZC
// All rights reserved.
// https://www.veriblock.org
// Distributed under the MIT software license, see the accompanying
// file LICENSE or http://www.opensource.org/licenses/mit-license.php.
package org.veriblock.miners.pop.service.mockmining

import org.slf4j.LoggerFactory
import org.veriblock.sdk.models.Address
import org.veriblock.sdk.models.BitcoinBlock
import org.veriblock.sdk.models.BitcoinTransaction
import org.veriblock.sdk.models.VeriBlockBlock
import org.veriblock.sdk.models.VeriBlockPoPTransaction
import org.veriblock.sdk.models.VeriBlockPublication
import org.veriblock.sdk.services.SerializeDeserializeService
import org.veriblock.sdk.util.Utils
import java.nio.ByteBuffer
import java.security.InvalidKeyException
import java.security.KeyPair
import java.security.NoSuchAlgorithmException
import java.security.PrivateKey
import java.security.SignatureException
import java.sql.SQLException

class VeriBlockPopMiner(
    val veriBlockBlockchain: VeriBlockMockBlockchain,
    val bitcoinBlockchain: BitcoinMockBlockchain
) {
    private fun createPublicationData(publishedBlock: VeriBlockBlock, address: Address): ByteArray {
        val buffer = ByteBuffer.allocateDirect(80)
        buffer.put(SerializeDeserializeService.serializeHeaders(publishedBlock))
        buffer.put(address.poPBytes)
        buffer.flip()
        val payoutInfo = ByteArray(80)
        buffer[payoutInfo]
        return payoutInfo
    }

    private fun createBitcoinTx(publishedBlock: VeriBlockBlock, address: Address): BitcoinTransaction {
        val publicationData = createPublicationData(publishedBlock, address)
        return BitcoinTransaction(publicationData)
    }

    @Throws(SignatureException::class, InvalidKeyException::class, NoSuchAlgorithmException::class)
    private fun signTransaction(tx: VeriBlockPoPTransaction, privateKey: PrivateKey): VeriBlockPoPTransaction {
        val signature = Utils.signMessageWithPrivateKey(
            SerializeDeserializeService.getId(tx).bytes,
            privateKey
        )
        return VeriBlockPoPTransaction(
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

    @Throws(
        SQLException::class, SignatureException::class, InvalidKeyException::class, NoSuchAlgorithmException::class
    )
    fun mine(
        blockToEndorse: VeriBlockBlock,
        lastKnownVBKBlock: VeriBlockBlock?,
        lastKnownBTCBlock: BitcoinBlock?,
        key: KeyPair
    ): VeriBlockPublication {
        log.debug("Mining")
        val address = Address.fromPublicKey(key.public.encoded)
        log.trace("Publish an endorsement transaction to Bitcoin")
        val bitcoinProofTx = createBitcoinTx(blockToEndorse, address)
        val btcBlockData = BitcoinBlockData()
        btcBlockData.add(bitcoinProofTx.rawBytes)
        val blockOfProof = bitcoinBlockchain.mine(btcBlockData)

        // create a VeriBlock PoP transaction
        val blockOfProofContext = bitcoinBlockchain.getContext(lastKnownBTCBlock!!)
        val popTx = signTransaction(
            VeriBlockPoPTransaction(
                address,
                blockToEndorse,
                bitcoinProofTx,
                btcBlockData.getMerklePath(0),
                blockOfProof,
                blockOfProofContext, ByteArray(1),
                key.public.encoded,
                veriBlockBlockchain.networkParameters.transactionPrefix
            ),
            key.private
        )
        log.trace("Publishing the PoP transaction to VeriBlock")
        val blockData = VeriBlockBlockData()
        blockData.popTransactions.add(popTx)
        val block = veriBlockBlockchain.mine(blockData)

        // create a VTB
        val context = veriBlockBlockchain.getContext(lastKnownVBKBlock!!)
        return VeriBlockPublication(
            popTx,
            blockData.getPoPMerklePath(0),
            block,
            context
        )
    }

    companion object {
        private val log = LoggerFactory.getLogger(VeriBlockPopMiner::class.java)
    }

}
