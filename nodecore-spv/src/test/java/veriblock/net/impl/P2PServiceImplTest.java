package veriblock.net.impl;

import org.junit.Before;
import org.junit.Test;
import org.veriblock.sdk.models.Coin;
import org.veriblock.sdk.models.Sha256Hash;
import veriblock.SpvContext;
import veriblock.conf.TestNetParameters;
import veriblock.model.Output;
import veriblock.model.StandardAddress;
import veriblock.model.StandardTransaction;
import veriblock.net.LocalhostDiscovery;
import veriblock.net.Peer;
import veriblock.service.PendingTransactionContainer;

import java.util.ArrayList;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class P2PServiceImplTest {

    private final SpvContext spvContext = new SpvContext();

    private PendingTransactionContainer pendingTransactionContainer;
    private Peer peer;
    private P2PServiceImpl p2PService;

    @Before
    public void setUp() {
        spvContext.init(TestNetParameters.get(), new LocalhostDiscovery(TestNetParameters.get()), false);

        this.pendingTransactionContainer = mock(PendingTransactionContainer.class);
        this.peer = mock(Peer.class);
        this.p2PService = new P2PServiceImpl(pendingTransactionContainer, spvContext.getNetworkParameters());
    }

    @Test
    public void onTransactionRequestWhenNotFindTx() {
        List<Sha256Hash> txIds = new ArrayList<>();
        txIds.add(Sha256Hash.ZERO_HASH);

        when(pendingTransactionContainer.getTransaction(any())).thenReturn(null);
        doNothing().when(peer).sendMessage(any());

        p2PService.onTransactionRequest(txIds, peer);

        verify(pendingTransactionContainer,  times(1)).getTransaction(any());
        verify(peer,  times(1)).sendMessage(any());
    }

    @Test
    public void onTransactionRequestWhenFindTx() {
        List<Sha256Hash> txIds = new ArrayList<>();
        txIds.add(Sha256Hash.ZERO_HASH);

        ArrayList<Output> outputs = new ArrayList<>();
        outputs.add(new Output(new StandardAddress("V7GghFKRA6BKqtHD7LTdT2ao93DRNA"), Coin.valueOf(3499999999L)));
        StandardTransaction standardTransaction =
            new StandardTransaction("V8dy5tWcP7y36kxiJwxKPKUrWAJbjs", 3500000000L, outputs, 5904L, spvContext.getNetworkParameters());
        byte[] pub = new byte[]{1, 2, 3};
        byte[] sign = new byte[]{3,2,1};
        standardTransaction.addSignature(sign,pub);

        when(pendingTransactionContainer.getTransaction(txIds.get(0))).thenReturn(standardTransaction);
        doNothing().when(peer).sendMessage(any());

        p2PService.onTransactionRequest(txIds, peer);

        verify(pendingTransactionContainer,  times(1)).getTransaction(txIds.get(0));
        verify(peer,  times(1)).sendMessage(any());
    }

}
