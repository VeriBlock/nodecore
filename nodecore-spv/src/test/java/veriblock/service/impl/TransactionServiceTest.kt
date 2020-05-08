package veriblock.service.impl

import io.mockk.mockk
import junit.framework.TestCase
import org.junit.Assert
import org.junit.Test
import org.veriblock.core.contracts.AddressManager
import org.veriblock.sdk.models.Coin
import veriblock.SpvContext
import veriblock.conf.TestNetParameters
import veriblock.model.AddressCoinsIndex
import veriblock.model.Output
import veriblock.model.StandardAddress
import veriblock.model.Transaction
import veriblock.net.LocalhostDiscovery
import veriblock.service.TransactionService

class TransactionServiceTest : TestCase() {
    private val spvContext = SpvContext()

    public override fun setUp() {
        spvContext.init(TestNetParameters, LocalhostDiscovery(TestNetParameters), false)
    }

    @Test
    fun testCreateTransactionsByOutputList() {
        val transactionService = TransactionService(mockk(relaxed = true), TestNetParameters)
        val addressCoinsIndexList = listOf(
            AddressCoinsIndex("V9YtYGe28er1D79qkshWHcxfbH3p2j", 100L * Coin.COIN_VALUE, 1L),
            AddressCoinsIndex("V66n5xh5Mu8nnR1D3is3eRkp92ktL9", 0L, 1L),
            AddressCoinsIndex("VBnNjRioQoFxVpvHuCd7eXo2jBZXj2", 300L * Coin.COIN_VALUE, 1L),
            AddressCoinsIndex("V4LmWfdThV2amLRtdGNBgBeic5ybhi", 100L * Coin.COIN_VALUE, 1L)
        )
        val outputList = listOf(
            Output(
                StandardAddress("V66n5xh5Mu8nnR1D3is3eRkp92ktL9"),
                Coin.valueOf(450L * Coin.COIN_VALUE)
            )
        )
        val result = transactionService!!.createTransactionsByOutputList(
            addressCoinsIndexList, outputList
        )
        Assert.assertNotNull(result)

        val totalResultOutPut = result.stream()
            .map { tx: Transaction ->
                tx.outputs.stream()
                    .map { o: Output -> o.amount.atomicUnits }
                    .reduce(0L) { a: Long, b: Long -> java.lang.Long.sum(a, b) }
            }
            .reduce(0L) { a: Long, b: Long -> java.lang.Long.sum(a, b) }

        Assert.assertEquals(totalResultOutPut, 450L * Coin.COIN_VALUE)
    }
}
