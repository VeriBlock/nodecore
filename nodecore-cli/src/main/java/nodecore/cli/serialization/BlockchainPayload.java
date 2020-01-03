package nodecore.cli.serialization;

import com.google.gson.annotations.SerializedName;

public class BlockchainPayload {
    @SerializedName("best_length")
    public long bestLength;

    @SerializedName("longest_length")
    public long longestLength;
}
