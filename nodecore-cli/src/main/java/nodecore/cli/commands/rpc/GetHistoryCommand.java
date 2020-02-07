// VeriBlock NodeCore CLI
// Copyright 2017-2019 Xenios SEZC
// All rights reserved.
// https://www.veriblock.org
// Distributed under the MIT software license, see the accompanying
// file LICENSE or http://www.opensource.org/licenses/mit-license.php.

package nodecore.cli.commands.rpc;

import io.grpc.StatusRuntimeException;
import nodecore.api.grpc.VeriBlockMessages;
import nodecore.api.grpc.utilities.ByteStringAddressUtility;
import nodecore.cli.annotations.CommandParameterType;
import nodecore.cli.annotations.CommandSpec;
import nodecore.cli.annotations.CommandSpecParameter;
import nodecore.cli.commands.serialization.AddressHistoryInfo;
import nodecore.cli.commands.serialization.FormattableObject;
import nodecore.cli.contracts.Command;
import nodecore.cli.contracts.CommandContext;
import nodecore.cli.contracts.DefaultResult;
import nodecore.cli.contracts.Result;
import nodecore.cli.utilities.CommandUtility;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

@CommandSpec(
        name = "Get History",
        form = "gethistory",
        description = "Gets transaction history for an address")
@CommandSpecParameter(name = "address", required = false, type = CommandParameterType.STANDARD_ADDRESS)
public class GetHistoryCommand implements Command {
    private static final Logger _logger = LoggerFactory.getLogger(GetHistoryCommand.class);

    public GetHistoryCommand() {
    }

    @Override
    public Result execute(CommandContext context) {
        Result result = new DefaultResult();

        String address = context.getParameter("address");
        try {
            VeriBlockMessages.GetHistoryRequest.Builder requestBuilder = VeriBlockMessages.GetHistoryRequest.newBuilder();
            if (address != null)
                requestBuilder.addAddresses(ByteStringAddressUtility.createProperByteStringAutomatically(address));
            VeriBlockMessages.GetHistoryReply reply = context
                    .adminService()
                    .getHistory(requestBuilder.build());
            if (!reply.getSuccess()) {
                result.fail();
            } else {
                FormattableObject<List<AddressHistoryInfo>> temp = new FormattableObject<>(reply.getResultsList());
                temp.success = !result.didFail();
                temp.payload = reply.getAddressesList()
                        .stream()
                        .map(AddressHistoryInfo::new).collect(Collectors.toList());

                context.outputObject(temp);

                context.suggestCommands(Arrays.asList(
                        GetBalanceCommand.class,
                        GetTransactionCommand.class
                ));
            }
            for (VeriBlockMessages.Result r : reply.getResultsList())
                result.addMessage(r.getCode(), r.getMessage(), r.getDetails(), r.getError());
        } catch (StatusRuntimeException e) {
            CommandUtility.handleRuntimeException(result, e, _logger);
        }

        return result;
    }
}
