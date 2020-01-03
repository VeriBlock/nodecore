package nodecore.cli.serialization;

import com.google.gson.annotations.SerializedName;
import nodecore.api.grpc.VeriBlockMessages;

import java.util.ArrayList;
import java.util.List;

public class BalancePayload {
    public BalancePayload(final VeriBlockMessages.GetBalanceReply reply) {
        for (final VeriBlockMessages.AddressBalance balanceInfo : reply.getConfirmedList())
            confirmed.add(new AddressBalanceInfo(balanceInfo));
        for (final VeriBlockMessages.Output output : reply.getUnconfirmedList())
            unconfirmed.add(new OutputInfo(output));
    }
    @SerializedName("confirmed")
    List<AddressBalanceInfo> confirmed = new ArrayList<>();
    @SerializedName("unconfirmed")
    List<OutputInfo> unconfirmed = new ArrayList<>();
}
