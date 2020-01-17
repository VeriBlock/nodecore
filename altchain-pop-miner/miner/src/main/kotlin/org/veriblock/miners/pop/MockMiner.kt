package org.veriblock.miners.pop

import org.veriblock.core.utilities.createLogger
import org.veriblock.core.utilities.extensions.toHex
import org.veriblock.lite.core.Balance
import org.veriblock.miners.pop.core.MiningOperation
import org.veriblock.miners.pop.service.PluginService
import org.veriblock.sdk.blockchain.store.BitcoinStore
import org.veriblock.sdk.blockchain.store.VeriBlockStore
import org.veriblock.sdk.mock.BitcoinBlockchain
import org.veriblock.sdk.mock.BitcoinDefaults
import org.veriblock.sdk.mock.VeriBlockBlockData
import org.veriblock.sdk.mock.VeriBlockBlockchain
import org.veriblock.sdk.mock.VeriBlockDefaults
import org.veriblock.sdk.mock.VeriBlockPopMiner
import org.veriblock.sdk.models.Address
import org.veriblock.sdk.models.AltPublication
import org.veriblock.sdk.models.Coin
import org.veriblock.sdk.models.PublicationData
import org.veriblock.sdk.models.Sha256Hash
import org.veriblock.sdk.models.VeriBlockBlock
import org.veriblock.sdk.models.VeriBlockTransaction
import org.veriblock.sdk.services.SerializeDeserializeService
import org.veriblock.sdk.sqlite.ConnectionSelector
import org.veriblock.sdk.util.Base58
import org.veriblock.sdk.util.KeyGenerator
import org.veriblock.sdk.util.Utils
import org.veriblock.shell.core.Result
import org.veriblock.shell.core.failure
import org.veriblock.shell.core.success
import java.nio.charset.StandardCharsets
import java.security.InvalidKeyException
import java.security.KeyPair
import java.security.NoSuchAlgorithmException
import java.security.PrivateKey
import java.security.PublicKey
import java.security.SignatureException
import java.sql.SQLException
import java.util.*

private val logger = createLogger {}

class MockMiner(
    private val pluginFactory: PluginService
) : Miner {
    private val veriBlockStore = VeriBlockStore(ConnectionSelector.setConnectionInMemory())
    private val bitcoinStore = BitcoinStore(ConnectionSelector.setConnectionInMemory())
    private val veriBlockBlockchain = VeriBlockBlockchain(VeriBlockDefaults.networkParameters, veriBlockStore, bitcoinStore)
    private val bitcoinBlockchain = BitcoinBlockchain(BitcoinDefaults.networkParameters, bitcoinStore)
    private val vpm = VeriBlockPopMiner(veriBlockBlockchain, bitcoinBlockchain)

    override fun start() {
        logger.info { "Mock mining enabled!"}
        veriBlockBlockchain.bootstrap(VeriBlockDefaults.bootstrap)
        logger.info { "Mocked VeriBlock chain bootstrap: ${VeriBlockDefaults.bootstrap.blocks.joinToString { it.raw.toHex() }}"}
        bitcoinBlockchain.bootstrap(BitcoinDefaults.bootstrap)
        logger.info { "Mocked Bitcoin chain bootstrap: ${BitcoinDefaults.bootstrap.blocks.joinToString { it.raw.toHex() }}"}
    }

    override fun shutdown() {
    }

    override fun mine(chainId: String, block: Int?): Result {
        val chain = pluginFactory[chainId]
        if (chain == null) {
            logger.warn { "Unable to load plugin $chainId" }
            return failure()
        }

        val publicationData = chain.getPublicationData(block)

        val key = KeyGenerator.generate()
        val atv = mine(publicationData.publicationData, veriBlockBlockchain.chainHead, key)

        val vtb = vpm.mine(
            veriBlockBlockchain.chainHead,
            veriBlockBlockchain.chainHead,
            bitcoinBlockchain.chainHead,
            key
        )

        val submissionResult = chain.submit(atv, listOf(vtb))
        logger.info { "Mock mine operation completed successfully! Result: $submissionResult" }

        return success()
    }


    // retrieve the blocks between lastKnownBlock and getChainHead()
    @Throws(SQLException::class)
    fun createVeriBlockContext(lastKnownBlock: VeriBlockBlock): List<VeriBlockBlock?> {
        val context: MutableList<VeriBlockBlock?> = ArrayList()
        var prevBlock = veriBlockBlockchain[veriBlockBlockchain.chainHead.previousBlock]
        while (prevBlock != null && prevBlock != lastKnownBlock) {
            context.add(prevBlock)
            prevBlock = veriBlockBlockchain[prevBlock.previousBlock]
        }
        Collections.reverse(context)
        return context
    }

    @Throws(SignatureException::class, InvalidKeyException::class, NoSuchAlgorithmException::class)
    private fun signTransaction(tx: VeriBlockTransaction, privateKey: PrivateKey): VeriBlockTransaction {
        val signature = Utils.signMessageWithPrivateKey(SerializeDeserializeService.getId(tx).bytes,
            privateKey)
        return VeriBlockTransaction(
            tx.type,
            tx.sourceAddress,
            tx.sourceAmount,
            tx.outputs,
            tx.signatureIndex,
            tx.publicationData,
            signature,
            tx.publicKey,
            tx.networkByte)
    }

    private fun deriveAddress(key: PublicKey): Address {
        val keyHash = Sha256Hash.of(key.encoded).bytes
        val data = "V" + Base58.encode(keyHash).substring(0, 24)
        val hash = Sha256Hash.of(data.toByteArray(StandardCharsets.UTF_8))
        val checksum = Base58.encode(hash.bytes).substring(0, 4 + 1)
        return Address(data + checksum)
    }

    @Throws(SQLException::class, SignatureException::class, InvalidKeyException::class, NoSuchAlgorithmException::class)
    fun mine(publicationData: PublicationData?, lastKnownVBKBlock: VeriBlockBlock, key: KeyPair): AltPublication {
        val address = deriveAddress(key.public)
        val endorsementTx = signTransaction(
            VeriBlockTransaction(
                1.toByte(),
                address,
                Coin.valueOf(1),
                ArrayList(),
                7,
                publicationData, ByteArray(1),
                key.public.encoded,
                veriBlockBlockchain.networkParameters.transactionMagicByte),
            key.private)
        // publish the endorsement transaction to VeriBlock
        val blockData = VeriBlockBlockData()
        blockData.regularTransactions.add(endorsementTx)
        val block = veriBlockBlockchain.mine(blockData)
        // create an ATV
        val context = createVeriBlockContext(lastKnownVBKBlock)
        return AltPublication(endorsementTx,
            blockData.getRegularMerklePath(0),
            block,
            context)
    }

    override fun listOperations(): List<String> = emptyList()

    override fun getOperation(id: String): MiningOperation? = null

    override fun getAddress(): String = "NO ADDRESS"

    override fun getBalance(): Balance? = null
}
