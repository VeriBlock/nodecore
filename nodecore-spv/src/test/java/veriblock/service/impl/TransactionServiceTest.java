package veriblock.service.impl;

import junit.framework.TestCase;
import org.junit.Assert;
import org.junit.Test;
import org.veriblock.core.contracts.AddressManager;
import org.veriblock.sdk.models.Coin;
import veriblock.SpvContext;
import veriblock.conf.TestNetParameters;
import veriblock.model.AddressCoinsIndex;
import veriblock.model.Output;
import veriblock.model.StandardAddress;
import veriblock.model.Transaction;
import veriblock.net.LocalhostDiscovery;

import java.util.ArrayList;
import java.util.List;

import static org.mockito.Mockito.mock;

public class TransactionServiceTest extends TestCase {

    private final SpvContext spvContext = new SpvContext();
    private TransactionService transactionService;
    private AddressManager addressManager;

    @Override
    public void setUp() throws Exception {
        spvContext.init(TestNetParameters.get(), new LocalhostDiscovery(TestNetParameters.get()), false);

        this.addressManager = mock(AddressManager.class);
        this.transactionService = new TransactionService(addressManager, TestNetParameters.get());
    }

    @Test
    public void testCreateTransactionsByOutputList() {
        List<AddressCoinsIndex> addressCoinsIndexList = new ArrayList<>();
        List<Output> outputList = new ArrayList<>();
        addressCoinsIndexList.add(new AddressCoinsIndex("V9YtYGe28er1D79qkshWHcxfbH3p2j", 100L * Coin.COIN_VALUE, 1L));
        addressCoinsIndexList.add(new AddressCoinsIndex("V66n5xh5Mu8nnR1D3is3eRkp92ktL9", 0L, 1L));
        addressCoinsIndexList.add(new AddressCoinsIndex("VBnNjRioQoFxVpvHuCd7eXo2jBZXj2", 300L * Coin.COIN_VALUE, 1L));
        addressCoinsIndexList.add(new AddressCoinsIndex("V4LmWfdThV2amLRtdGNBgBeic5ybhi", 100L * Coin.COIN_VALUE, 1L));

        outputList.add(new Output(new StandardAddress("V66n5xh5Mu8nnR1D3is3eRkp92ktL9"), Coin.valueOf(450L * Coin.COIN_VALUE)));

        List<Transaction> result = transactionService.createTransactionsByOutputList(addressCoinsIndexList, outputList);

        Assert.assertNotNull(result);

        long totalResultOutPut = result.stream()
            .map(tx -> tx.getOutputs().stream()
                .map(o -> o.getAmount().getAtomicUnits())
                .reduce(0L, Long::sum)
            )
            .reduce(0L, Long::sum);

        Assert.assertEquals(totalResultOutPut, 450L * Coin.COIN_VALUE);
    }
}
