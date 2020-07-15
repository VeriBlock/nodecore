package org.veriblock.miners.pop.service.mockmining

import kotlinx.coroutines.runBlocking
import org.veriblock.core.NotImplementedException
import org.veriblock.core.bitcoinj.Base58
import org.veriblock.core.crypto.Sha256Hash
import org.veriblock.core.params.defaultRegTestParameters
import org.veriblock.core.utilities.createLogger
import org.veriblock.core.utilities.extensions.toHex
import org.veriblock.core.contracts.Balance
import org.veriblock.lite.transactionmonitor.WalletTransaction
import org.veriblock.miners.pop.core.ApmOperation
import org.veriblock.miners.pop.core.ApmSpTransaction
import org.veriblock.miners.pop.securityinheriting.SecurityInheritingService
import org.veriblock.miners.pop.service.MinerService
import org.veriblock.sdk.alt.plugin.PluginService
import org.veriblock.sdk.blockchain.store.BitcoinStore
import org.veriblock.sdk.blockchain.store.VeriBlockStore
import org.veriblock.sdk.models.Address
import org.veriblock.sdk.models.AltPublication
import org.veriblock.sdk.models.PublicationData
import org.veriblock.core.crypto.VBlakeHash
import org.veriblock.sdk.models.VeriBlockBlock
import org.veriblock.sdk.models.VeriBlockTransaction
import org.veriblock.sdk.models.asCoin
import org.veriblock.sdk.services.SerializeDeserializeService
import org.veriblock.sdk.sqlite.ConnectionSelector
import org.veriblock.core.utilities.Utility
import org.veriblock.miners.pop.core.MiningOperationStatus
import java.nio.charset.StandardCharsets
import java.security.InvalidAlgorithmParameterException
import java.security.KeyPair
import java.security.KeyPairGenerator
import java.security.NoSuchAlgorithmException
import java.security.PrivateKey
import java.security.PublicKey
import java.security.SecureRandom
import java.security.spec.ECGenParameterSpec
import java.util.ArrayList
import java.util.HashMap

private val logger = createLogger {}

class MockMinerService(
    private val pluginService: PluginService,
    private val securityInheritingService: SecurityInheritingService
) : MinerService {
    private val bootstrap = listOf(defaultRegTestParameters.genesisBlock)
    private val btcBootstrap = listOf(BitcoinDefaults.genesis)
    private val connection = ConnectionSelector.setConnection("mock")
    private val veriBlockStore = VeriBlockStore(connection)
    private val bitcoinStore = BitcoinStore(connection)
    private val veriBlockBlockchain = VeriBlockMockBlockchain(
        defaultRegTestParameters, veriBlockStore, bitcoinStore
    )
    private val bitcoinBlockchain = BitcoinMockBlockchain(BitcoinDefaults.networkParameters, bitcoinStore)
    private val vpm = VeriBlockPopMiner(veriBlockBlockchain, bitcoinBlockchain)

    private val operations = HashMap<String, ApmOperation>()

    override fun initialize() {
        logger.info { "Mock mining enabled!" }
    }

    override fun start() {
        veriBlockBlockchain.bootstrap(bootstrap)
        logger.info { "Mocked VeriBlock chain bootstrap: ${bootstrap.joinToString { it.raw.toHex() }}" }
        bitcoinBlockchain.bootstrap(btcBootstrap, BitcoinDefaults.genesisHeight)
        logger.info { "Mocked Bitcoin chain bootstrap: ${btcBootstrap.joinToString { it.raw.toHex() }}" }
    }

    override fun setIsShuttingDown(b: Boolean) {
    }

    override fun shutdown() {
    }

    override fun mine(chainId: String, block: Int?): String {
        val chain = pluginService[chainId]
            ?: error("Unable to load plugin $chainId")

        val chainMonitor = securityInheritingService.getMonitor(chainId)
            ?: error("Unable to load altchain monitor $chainId")

        val operation = ApmOperation(
            chain = chain,
            chainMonitor = chainMonitor
        )
        operations[operation.id] = operation

        val miningInstruction = try {
            runBlocking {
                chain.getMiningInstruction(block)
            }
        } catch (e: Exception) {
            operation.fail(e.message ?: "Unknown reason")
            throw e
        }

        operation.setMiningInstruction(miningInstruction)

        val key = generateKeyPair()

        val vbkTip = veriBlockBlockchain.getChainHead()!!
        val atv = mine(miningInstruction.publicationData, vbkTip, key)

        // Update operation state
        operation.setTransaction(ApmSpTransaction(WalletTransaction.wrap(atv.transaction)))
        operation.setConfirmed()
        operation.setBlockOfProof(atv.containingBlock)
        operation.setMerklePath(atv.merklePath)

        val lastKnownBtcBlockHash = miningInstruction.btcContext.last()
        val lastKnownVbkBlockHash = miningInstruction.context.last()
        val lastKnownBtcBlock = bitcoinBlockchain.get(Sha256Hash.wrap(lastKnownBtcBlockHash))!!
        val lastKnownVbkBlock = veriBlockBlockchain.get(VBlakeHash.wrap(lastKnownVbkBlockHash))!!

        val vtb = vpm.mine(
            vbkTip,
            lastKnownVbkBlock,
            lastKnownBtcBlock,
            key
        )
        val vtbs = listOf(vtb)
        operation.setContext(vtbs)

        val submissionResult = try {
            runBlocking {
                chain.submit(atv, vtbs)
            }
        } catch (e: Exception) {
            operation.fail(e.message ?: "Unknown reason")
            throw e
        }
        operation.setPopTxId(submissionResult)
        logger.info { "Mock mine operation completed successfully! Result: $submissionResult" }

        // TODO: Rework mock miner so that it actually just mocks the nodecore gateway and then delete this whole class
        operation.setPayoutData("", 0)
        operation.complete()
        return operation.id
    }

    private fun createVeriBlockContext(lastKnownBlock: VeriBlockBlock): List<VeriBlockBlock> {
        // Retrieve the blocks between lastKnownBlock and getChainHead()
        val context: MutableList<VeriBlockBlock> = ArrayList()
        var prevBlock = veriBlockBlockchain.get(veriBlockBlockchain.getChainHead()!!.previousBlock)
        while (prevBlock != null && prevBlock != lastKnownBlock) {
            context.add(prevBlock)
            prevBlock = veriBlockBlockchain.get(prevBlock.previousBlock)
        }
        context.reverse()
        return context
    }

    private fun signTransaction(tx: VeriBlockTransaction, privateKey: PrivateKey): VeriBlockTransaction {
        val signature = Utility.signMessageWithPrivateKey(
            SerializeDeserializeService.getId(tx).bytes,
            privateKey
        )
        return VeriBlockTransaction(
            tx.type,
            tx.sourceAddress,
            tx.sourceAmount,
            tx.outputs,
            tx.signatureIndex,
            tx.publicationData,
            signature,
            tx.publicKey,
            tx.networkByte
        )
    }

    private fun deriveAddress(key: PublicKey): Address {
        val keyHash = Sha256Hash.of(key.encoded).bytes
        val data = "V" + Base58.encode(keyHash).substring(0, 24)
        val hash = Sha256Hash.of(data.toByteArray(StandardCharsets.UTF_8))
        val checksum = Base58.encode(hash.bytes).substring(0, 4 + 1)
        return Address(data + checksum)
    }

    private fun mine(publicationData: PublicationData, lastKnownVBKBlock: VeriBlockBlock, key: KeyPair): AltPublication {
        val address = deriveAddress(key.public)
        val endorsementTx = signTransaction(
            VeriBlockTransaction(
                1.toByte(),
                address,
                1.asCoin(),
                ArrayList(),
                7,
                publicationData,
                ByteArray(1),
                key.public.encoded,
                veriBlockBlockchain.networkParameters.transactionPrefix
            ),
            key.private
        )
        // publish the endorsement transaction to VeriBlock
        val blockData = VeriBlockBlockData()
        blockData.regularTransactions.add(endorsementTx)
        val block = veriBlockBlockchain.mine(blockData)
        // create an ATV
        val context = createVeriBlockContext(lastKnownVBKBlock)
        return AltPublication(
            endorsementTx,
            blockData.getRegularMerklePath(0),
            block,
            context
        )
    }

    override fun resubmit(operation: ApmOperation) =
        throw NotImplementedException("Operation not supported in the Mock Miner")

    override fun cancelOperation(id: String) =
        throw NotImplementedException("Operation not supported in the Mock Miner")

    override fun getOperations(status: MiningOperationStatus, limit: Int, offset: Int): List<ApmOperation> =
        operations.values.sortedBy { it.createdAt }

    override fun getOperationsCount(status: MiningOperationStatus): Int =
        operations.size

    override fun getOperation(id: String): ApmOperation? =
        operations[id]

    override fun getAddress(): String =
        "NO ADDRESS"

    override fun getBalance(): Balance? =
        null
}

@Throws(NoSuchAlgorithmException::class, InvalidAlgorithmParameterException::class)
private fun generateKeyPair(): KeyPair {
    val keyPairGenerator = KeyPairGenerator.getInstance("EC")
    val random = SecureRandom.getInstance("SHA1PRNG")
    random.setSeed(1L)
    keyPairGenerator.initialize(ECGenParameterSpec("secp256k1"), random)
    return keyPairGenerator.generateKeyPair()
}
