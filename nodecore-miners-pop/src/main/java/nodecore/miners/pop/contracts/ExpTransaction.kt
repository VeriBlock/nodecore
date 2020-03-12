package nodecore.miners.pop.contracts;

import org.bitcoinj.core.NetworkParameters;
import org.bitcoinj.core.Transaction;
import org.bitcoinj.core.UnsafeByteArrayOutputStream;

import java.io.ByteArrayOutputStream;
import java.io.IOException;

public class ExpTransaction extends Transaction {
    public ExpTransaction(NetworkParameters params, byte[] payloadBytes) {
        super(params, payloadBytes);
    }

    public byte[] getFilteredTransaction() {
        ByteArrayOutputStream stream = new UnsafeByteArrayOutputStream(length < 32 ? 32 : length + 32);
        try {
            bitcoinSerializeToStream(stream, false);
        } catch (IOException e) {
            throw new RuntimeException(e); // cannot happen
        }

        return stream.toByteArray();
    }
}
