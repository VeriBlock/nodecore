// VeriBlock NodeCore CLI
// Copyright 2017-2019 Xenios SEZC
// All rights reserved.
// https://www.veriblock.org
// Distributed under the MIT software license, see the accompanying
// file LICENSE or http://www.opensource.org/licenses/mit-license.php.

package nodecore.cli.commands.rpc;

import com.google.gson.annotations.SerializedName;
import io.grpc.StatusRuntimeException;
import nodecore.api.grpc.VeriBlockMessages;
import nodecore.api.grpc.utilities.ByteStringAddressUtility;
import nodecore.cli.annotations.CommandParameterType;
import nodecore.cli.annotations.CommandSpec;
import nodecore.cli.annotations.CommandSpecParameter;
import nodecore.cli.commands.serialization.AddressBalanceInfo;
import nodecore.cli.commands.serialization.FormattableObject;
import nodecore.cli.commands.serialization.OutputInfo;
import nodecore.cli.contracts.Command;
import nodecore.cli.contracts.CommandContext;
import nodecore.cli.contracts.DefaultResult;
import nodecore.cli.contracts.Result;
import nodecore.cli.utilities.CommandUtility;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

@CommandSpec(
        name = "Get Balance",
        form = "getbalance",
        description = "See the balances of all of your addresses")
@CommandSpecParameter(name = "address", required = false, type = CommandParameterType.STANDARD_OR_MULTISIG_ADDRESS)
public class GetBalanceCommand implements Command {
    private static final Logger _logger = LoggerFactory.getLogger(GetBalanceCommand.class);

    private class BalancePayload {
        BalancePayload(final VeriBlockMessages.GetBalanceReply reply) {
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

    public GetBalanceCommand() {
    }

    @Override
    public Result execute(CommandContext context) {
        Result result = new DefaultResult();

        String address = context.getParameter("address");
        try {
            VeriBlockMessages.GetBalanceRequest.Builder requestBuilder = VeriBlockMessages.GetBalanceRequest.newBuilder();
            if (address != null) {
                requestBuilder.addAddresses(ByteStringAddressUtility.createProperByteStringAutomatically(address));
            }
            VeriBlockMessages.GetBalanceReply reply = context
                    .adminService()
                    .getBalance(requestBuilder.build());
            if (!reply.getSuccess()) {
                result.fail();
            } else {
                FormattableObject<BalancePayload> temp = new FormattableObject<>(reply.getResultsList());
                temp.success = !result.didFail();
                temp.payload = new BalancePayload(reply);

                context.outputObject(temp);

                context.suggestCommands(Arrays.asList(
                        GetHistoryCommand.class,
                        GetNewAddressCommand.class));

            }
            for (VeriBlockMessages.Result r : reply.getResultsList())
                result.addMessage(r.getCode(), r.getMessage(), r.getDetails(), r.getError());
        } catch (StatusRuntimeException e) {
            CommandUtility.handleRuntimeException(result, e, _logger);

        }

        return result;
    }
}
