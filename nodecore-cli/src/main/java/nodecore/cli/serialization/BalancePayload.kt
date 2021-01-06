package nodecore.cli.serialization

import com.google.gson.annotations.SerializedName
import nodecore.api.grpc.VeriBlockMessages.GetBalanceReply
import org.veriblock.core.utilities.Utility

class BalancePayload(
    reply: GetBalanceReply
) {
    @SerializedName("confirmed")
    val confirmed = reply.confirmedList.map {
        AddressBalanceInfo(it)
    }

    @SerializedName("total_confirmed")
    val totalConfirmed = Utility.formatAtomicLongWithDecimal(reply.totalConfirmed)

    @SerializedName("unconfirmed")
    val unconfirmed = reply.unconfirmedList.map { output ->
        OutputInfo(output)
    }

    @SerializedName("total_unconfirmed")
    val totalUnconfirmed = Utility.formatAtomicLongWithDecimal(reply.totalUnconfirmed)
}
