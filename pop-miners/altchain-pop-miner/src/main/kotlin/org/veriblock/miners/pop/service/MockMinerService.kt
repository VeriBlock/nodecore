package org.veriblock.miners.pop.service

import org.veriblock.core.utilities.createLogger
import org.veriblock.core.utilities.extensions.toHex
import org.veriblock.lite.core.Balance
import org.veriblock.lite.transactionmonitor.WalletTransaction
import org.veriblock.miners.pop.core.ApmContext
import org.veriblock.miners.pop.core.ApmMerklePath
import org.veriblock.miners.pop.core.ApmOperation
import org.veriblock.miners.pop.core.ApmSpBlock
import org.veriblock.miners.pop.core.ApmSpTransaction
import org.veriblock.miners.pop.securityinheriting.SecurityInheritingService
import org.veriblock.sdk.alt.plugin.PluginService
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
import org.veriblock.sdk.models.VBlakeHash
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
import java.security.KeyPair
import java.security.PrivateKey
import java.security.PublicKey
import java.util.ArrayList
import java.util.HashMap

private val logger = createLogger {}

class MockMinerService(
    private val pluginService: PluginService,
    private val securityInheritingService: SecurityInheritingService
) : MinerService {
    private val connection = ConnectionSelector.setConnection("mock")
    private val veriBlockStore = VeriBlockStore(connection)
    private val bitcoinStore = BitcoinStore(connection)
    private val veriBlockBlockchain = VeriBlockBlockchain(VeriBlockDefaults.networkParameters, veriBlockStore, bitcoinStore)
    private val bitcoinBlockchain = BitcoinBlockchain(BitcoinDefaults.networkParameters, bitcoinStore)
    private val vpm = VeriBlockPopMiner(veriBlockBlockchain, bitcoinBlockchain)

    private val operations = HashMap<String, ApmOperation>()

    override fun initialize() {
        logger.info { "Mock mining enabled!" }
    }

    override fun start() {
        veriBlockBlockchain.bootstrap(VeriBlockDefaults.bootstrap)
        logger.info { "Mocked VeriBlock chain bootstrap: ${VeriBlockDefaults.bootstrap.blocks.joinToString { it.raw.toHex() }}" }
        bitcoinBlockchain.bootstrap(BitcoinDefaults.bootstrap)
        logger.info { "Mocked Bitcoin chain bootstrap: ${BitcoinDefaults.bootstrap.blocks.joinToString { it.raw.toHex() }}" }
    }

    override fun setIsShuttingDown(b: Boolean) {
    }

    override fun shutdown() {
    }

    override fun mine(chainId: String, block: Int?): Result {
        val chain = pluginService[chainId]
        if (chain == null) {
            logger.warn { "Unable to load plugin $chainId" }
            return failure()
        }

        val chainMonitor = securityInheritingService.getMonitor(chainId)
            ?: error("Unable to load altchain monitor $chainId")

        val operation = ApmOperation(
            chain = chain,
            chainMonitor = chainMonitor
        )
        operations[operation.id] = operation

        val miningInstruction = try {
            chain.getMiningInstruction(block)
        } catch (e: Exception) {
            operation.fail(e.message ?: "Unknown reason")
            throw e
        }

        operation.setMiningInstruction(miningInstruction)

        val key = KeyGenerator.generate()

        val vbkTip = veriBlockBlockchain.chainHead
        val atv = mine(miningInstruction.publicationData, vbkTip, key)

        // Update operation state
        operation.setTransaction(ApmSpTransaction(WalletTransaction.wrap(atv.transaction)))
        operation.setConfirmed()
        operation.setBlockOfProof(ApmSpBlock(atv.containingBlock))
        operation.setMerklePath(ApmMerklePath(atv.merklePath))

        val lastKnownBtcBlockHash = miningInstruction.btcContext.last()
        val lastKnownVbkBlockHash = miningInstruction.context.last()
        val lastKnownBtcBlock = bitcoinBlockchain[Sha256Hash.wrap(lastKnownBtcBlockHash)]
        val lastKnownVbkBlock = veriBlockBlockchain[VBlakeHash.wrap(lastKnownVbkBlockHash)]

        val vtb = vpm.mine(
            vbkTip,
            lastKnownVbkBlock,
            lastKnownBtcBlock,
            key
        )
        val vtbs = listOf(vtb)
        operation.setContext(ApmContext(vtbs))

        val submissionResult = try {
            chain.submit(atv, vtbs)
        } catch (e: Exception) {
            operation.fail(e.message ?: "Unknown reason")
            throw e
        }
        operation.setProofOfProofId(submissionResult)
        logger.info { "Mock mine operation completed successfully! Result: $submissionResult" }

        // TODO: Rework mock miner so that it actually just mocks the nodecore gateway and then delete this whole class
        operation.complete("", 0)
        return success()
    }

    private fun createVeriBlockContext(lastKnownBlock: VeriBlockBlock): List<VeriBlockBlock> {
        // Retrieve the blocks between lastKnownBlock and getChainHead()
        val context: MutableList<VeriBlockBlock> = ArrayList()
        var prevBlock = veriBlockBlockchain[veriBlockBlockchain.chainHead.previousBlock]
        while (prevBlock != null && prevBlock != lastKnownBlock) {
            context.add(prevBlock)
            prevBlock = veriBlockBlockchain[prevBlock.previousBlock]
        }
        context.reverse()
        return context
    }

    private fun signTransaction(tx: VeriBlockTransaction, privateKey: PrivateKey): VeriBlockTransaction {
        val signature = Utils.signMessageWithPrivateKey(
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

    private fun mine(publicationData: PublicationData?, lastKnownVBKBlock: VeriBlockBlock, key: KeyPair): AltPublication {
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
                veriBlockBlockchain.networkParameters.transactionMagicByte
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
        error("Operation not supported in the Mock Miner")

    override fun cancelOperation(id: String) =
        error("Operation not supported in the Mock Miner")

    override fun getOperations(): List<ApmOperation> =
        operations.values.sortedBy { it.createdAt }

    override fun getOperation(id: String): ApmOperation? =
        operations[id]

    override fun getAddress(): String =
        "NO ADDRESS"

    override fun getBalance(): Balance? =
        null
}
