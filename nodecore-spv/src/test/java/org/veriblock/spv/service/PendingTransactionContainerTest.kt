package org.veriblock.spv.service

import com.google.protobuf.ByteString
import io.kotest.matchers.shouldNotBe
import io.mockk.every
import io.mockk.mockk
import nodecore.api.grpc.RpcSignedTransaction
import nodecore.api.grpc.RpcTransaction
import nodecore.api.grpc.RpcTransactionUnion
import org.junit.Before
import org.junit.Test
import org.veriblock.core.Context
import org.veriblock.core.params.defaultTestNetParameters
import org.veriblock.core.utilities.AddressUtility
import org.veriblock.core.utilities.extensions.toHex
import org.veriblock.core.wallet.AddressKeyGenerator
import org.veriblock.spv.SpvConfig
import org.veriblock.spv.SpvContext
import kotlin.random.Random

private const val KB = 1024

class PendingTransactionContainerTest {
    private lateinit var spvContext: SpvContext
    private lateinit var pendingTransactionContainer: PendingTransactionContainer

    private val ledger = mutableMapOf<String, AddressData>()
    private val pendingLedger = mutableMapOf<String, AddressData>()

    data class AddressData(
        val address: String,
        var balance: Long = 50_000_000,
        var sigIndex: Long = 0L
    )

    private fun String.getAddressData() = ledger.getOrPut(this) { AddressData(this) }
    private fun String.getPendingAddressData() = pendingLedger.getOrPut(this) { getAddressData().copy() }

    @Before
    fun setUp() {
        Context.set(defaultTestNetParameters)
        spvContext = SpvContext(SpvConfig(defaultTestNetParameters, connectDirectlyTo = listOf("localhost")))
        spvContext.wallet.newAddress
        pendingTransactionContainer = PendingTransactionContainer(spvContext)
    }

    private fun createRpcTransactionUnionMock(
        txCase: RpcTransactionUnion.TransactionCase = RpcTransactionUnion.TransactionCase.SIGNED,
        tx: RpcSignedTransaction = createRpcSignedTransactionMock(),
    ): RpcTransactionUnion = mockk {
        every { transactionCase } returns txCase
        every { signed } returns tx
    }

    private fun createRpcSignedTransactionMock(
        pubKey: ByteString = ByteString.copyFromUtf8(spvContext.wallet.defaultAddress.hash),
        sigIndex: Long = ++pubKey.toString().getPendingAddressData().sigIndex,
        tx: RpcTransaction = createRpcTransactionMock(
            srcAddress = pubKey,
        ),
    ): RpcSignedTransaction = mockk {
        every { transaction } returns tx
        every { publicKey } returns pubKey
        every { signature } returns ByteString.copyFrom(spvContext.wallet.signMessage(tx.txId.toByteArray(), pubKey.toStringUtf8()))
        every { signatureIndex } returns sigIndex
    }

    private fun createRpcTransactionMock(
        typeTx: RpcTransaction.Type = RpcTransaction.Type.STANDARD,
        id: ByteString = ByteString.copyFromUtf8(Random.nextBytes(32).toHex()),
        srcAddress: ByteString = ByteString.copyFromUtf8(generateRandomAddress()),
        amount: Long = 1000L,
        valid: Boolean = true,
        signed: Boolean = true,
        fee: Long = 1_000_000L,
        dataTx: ByteString = ByteString.copyFromUtf8(""),
        ): RpcTransaction = mockk {
        every { type } returns typeTx
        every { txId } returns id
        every { sourceAddress } returns srcAddress
        every { sourceAmount } returns amount
//        every { isSigned } returns signed
//        every { isValid } returns valid
        every { outputsList } returns emptyList()
//        every { timeStamp } returns getCurrentTimestamp()
        every { transactionFee } returns fee
        every { data } returns dataTx
        every { size } returns KB // 1KB per transaction
    }

    private fun generateRandomAddress(): String {
        val pair = AddressKeyGenerator.generate()
        return AddressUtility.addressFromPublicKey(pair.public)
    }

    @Test
    fun testAddNetworkTransaction() {
        val txUnion = createRpcTransactionUnionMock()
        val res = pendingTransactionContainer.addNetworkTransaction(txUnion)
        res shouldNotBe PendingTransactionContainer.AddTransactionResult.INVALID
    }
}
