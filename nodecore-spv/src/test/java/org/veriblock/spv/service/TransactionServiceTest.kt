package org.veriblock.spv.service

import io.mockk.mockk
import junit.framework.TestCase
import org.junit.Assert
import org.junit.Test
import org.veriblock.core.Context
import org.veriblock.core.params.defaultTestNetParameters
import org.veriblock.sdk.models.Coin
import org.veriblock.sdk.models.asCoinDecimal
import org.veriblock.spv.SpvConfig
import org.veriblock.spv.SpvContext
import org.veriblock.spv.model.AddressCoinsIndex
import org.veriblock.spv.model.Output
import org.veriblock.spv.model.StandardAddress

class TransactionServiceTest : TestCase() {
    private lateinit var spvContext: SpvContext

    public override fun setUp() {
        Context.set(defaultTestNetParameters)
        spvContext = SpvContext(SpvConfig("testnet", connectDirectlyTo = listOf("localhost")))
    }

    @Test
    fun testCreateTransactionsByOutputList() {
        val transactionService = TransactionService(mockk(relaxed = true), defaultTestNetParameters)
        val addressCoinsIndexList = listOf(
            AddressCoinsIndex("V9YtYGe28er1D79qkshWHcxfbH3p2j", 100L * Coin.COIN_VALUE, 1L),
            AddressCoinsIndex("V66n5xh5Mu8nnR1D3is3eRkp92ktL9", 0L, 1L),
            AddressCoinsIndex("VBnNjRioQoFxVpvHuCd7eXo2jBZXj2", 300L * Coin.COIN_VALUE, 1L),
            AddressCoinsIndex("V4LmWfdThV2amLRtdGNBgBeic5ybhi", 100L * Coin.COIN_VALUE, 1L)
        )
        val outputList = listOf(
            Output(
                StandardAddress("V66n5xh5Mu8nnR1D3is3eRkp92ktL9"),
                450.asCoinDecimal()
            )
        )
        val result = transactionService.createTransactionsByOutputList(
            addressCoinsIndexList, outputList
        )
        Assert.assertNotNull(result)

        val totalResultOutPut = result.map { tx ->
            tx.getOutputs().map {
                it.amount.atomicUnits
            }.sum()
        }.sum()

        Assert.assertEquals(totalResultOutPut, 450L * Coin.COIN_VALUE)
    }
}
