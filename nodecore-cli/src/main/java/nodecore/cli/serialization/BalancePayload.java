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
        total_confirmed = Utility.formatAtomicLongWithDecimal(reply.getTotalConfirmed());
        for (final VeriBlockMessages.Output output : reply.getUnconfirmedList()) {
            unconfirmed.add(new OutputInfo(output));
        }
        total_unconfirmed = Utility.formatAtomicLongWithDecimal(reply.getTotalUnconfirmed());
    }

    @SerializedName("confirmed")
    List<AddressBalanceInfo> confirmed = new ArrayList<>();

    @SerializedName("total_confirmed")
    public String total_confirmed;

    @SerializedName("unconfirmed")
    List<OutputInfo> unconfirmed = new ArrayList<>();

    @SerializedName("total_unconfirmed")
    public String total_unconfirmed;
}
