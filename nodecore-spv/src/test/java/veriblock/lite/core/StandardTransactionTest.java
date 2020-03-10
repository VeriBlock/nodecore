package veriblock.lite.core;

import nodecore.api.grpc.VeriBlockMessages;
import nodecore.api.grpc.utilities.ByteStringUtility;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.veriblock.core.utilities.Utility;
import org.veriblock.sdk.models.Coin;
import veriblock.SpvContext;
import veriblock.conf.MainNetParameters;
import veriblock.model.Output;
import veriblock.model.StandardAddress;
import veriblock.model.StandardTransaction;
import veriblock.net.LocalhostDiscovery;

import java.util.ArrayList;

public class StandardTransactionTest {

    private final SpvContext spvContext = new SpvContext();

    @Before
    public void setUp() {
        spvContext.init(MainNetParameters.get(), new LocalhostDiscovery(MainNetParameters.get()), false);
    }

    @Test
    public void getTransactionMessageBuilder() {
        ArrayList<Output> outputs = new ArrayList<>();
        outputs.add(new Output(new StandardAddress("V7GghFKRA6BKqtHD7LTdT2ao93DRNA"), Coin.valueOf(3499999999L)));

        StandardTransaction tx =
            new StandardTransaction("V8dy5tWcP7y36kxiJwxKPKUrWAJbjs", 3500000000L, outputs, 5904L, spvContext.getNetworkParameters());

        byte[] pub = new byte[]{1,2,3};
        byte[] sign = new byte[]{3,2,1};
        tx.addSignature(sign,pub);

        VeriBlockMessages.SignedTransaction signedTransaction = tx.getSignedMessageBuilder(spvContext.getNetworkParameters()).build();

        Assert.assertEquals(5904L, signedTransaction.getSignatureIndex());
        Assert.assertEquals(3500000000L, signedTransaction.getTransaction().getSourceAmount());
        Assert.assertEquals("V8dy5tWcP7y36kxiJwxKPKUrWAJbjs", ByteStringUtility.byteStringToBase58(signedTransaction.getTransaction().getSourceAddress()));
        Assert.assertEquals(1 , signedTransaction.getTransaction().getOutputsList().size());
        Assert.assertEquals("V7GghFKRA6BKqtHD7LTdT2ao93DRNA", ByteStringUtility.byteStringToBase58(signedTransaction.getTransaction().getOutputs(0).getAddress()));
        Assert.assertEquals(3499999999L, signedTransaction.getTransaction().getOutputs(0).getAmount());
        Assert.assertEquals(pub, tx.getPublicKey());
        Assert.assertEquals(sign, tx.getSignature());
    }

    @Test
    public void serialize() {
        ArrayList<Output> outputs = new ArrayList<>();
        outputs.add(new Output(new StandardAddress("V7GghFKRA6BKqtHD7LTdT2ao93DRNA"), Coin.valueOf(3499999999L)));

        StandardTransaction tx =
            new StandardTransaction("V8dy5tWcP7y36kxiJwxKPKUrWAJbjs", 3500000000L, outputs, 5904L, spvContext.getNetworkParameters());

        byte[] serialized = tx.toByteArray(spvContext.getNetworkParameters());

        Assert.assertEquals("01011667A654EE3E0C918D8652B63829D7F3BEF98524BF899604D09DC30001011667901A1E11C650509EFC46E09E81678054D8562AF02B04D09DC2FF0217100100", Utility.bytesToHex(serialized));
    }

}
