package nodecore.cli.serialization;

import com.google.gson.annotations.SerializedName;
import nodecore.api.grpc.VeriBlockMessages;
import org.veriblock.core.utilities.Utility;

import java.util.ArrayList;
import java.util.List;

public class BalancePayload {
    public BalancePayload(final VeriBlockMessages.GetBalanceReply reply) {
        for (final VeriBlockMessages.AddressBalance balanceInfo : reply.getConfirmedList()) {
            confirmed.add(new AddressBalanceInfo(balanceInfo));
        }
        totalConfirmed = Utility.formatAtomicLongWithDecimal(reply.getTotalConfirmed());
        for (final VeriBlockMessages.Output output : reply.getUnconfirmedList()) {
            unconfirmed.add(new OutputInfo(output));
        }
        totalUnconfirmed = Utility.formatAtomicLongWithDecimal(reply.getTotalUnconfirmed());
    }

    @SerializedName("confirmed")
    List<AddressBalanceInfo> confirmed = new ArrayList<>();

    @SerializedName("total_confirmed")
    public String totalConfirmed;

    @SerializedName("unconfirmed")
    List<OutputInfo> unconfirmed = new ArrayList<>();

    @SerializedName("total_unconfirmed")
    public String totalUnconfirmed;
}
