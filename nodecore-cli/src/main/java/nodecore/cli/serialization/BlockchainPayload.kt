package nodecore.cli.serialization

import com.google.gson.annotations.SerializedName

class BlockchainPayload {
    @SerializedName("best_length")
    var bestLength: Long = 0

    @SerializedName("longest_length")
    var longestLength: Long = 0
}
