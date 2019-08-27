// VeriBlock NodeCore CLI
// Copyright 2017-2019 Xenios SEZC
// All rights reserved.
// https://www.veriblock.org
// Distributed under the MIT software license, see the accompanying
// file LICENSE or http://www.opensource.org/licenses/mit-license.php.

package nodecore.cli.commands.rpc;

import com.google.inject.Inject;
import io.grpc.StatusRuntimeException;
import nodecore.api.grpc.GetNewAddressReply;
import nodecore.api.grpc.GetNewAddressRequest;
import nodecore.cli.annotations.CommandParameterType;
import nodecore.cli.annotations.CommandSpec;
import nodecore.cli.annotations.CommandSpecParameter;
import nodecore.cli.commands.serialization.FormattableObject;
import nodecore.cli.commands.serialization.NewAddressPayload;
import nodecore.cli.contracts.Command;
import nodecore.cli.contracts.CommandContext;
import nodecore.cli.contracts.DefaultResult;
import nodecore.cli.contracts.Result;
import nodecore.cli.utilities.CommandUtility;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Arrays;

@CommandSpec(
        name = "Get New Address",
        form = "getnewaddress",
        description = "Gets {count} new address from the wallet (default: 1)")
@CommandSpecParameter(name = "count", required = false, type = CommandParameterType.INTEGER)
public class GetNewAddressCommand implements Command {
    private static final Logger _logger = LoggerFactory.getLogger(GetNewAddressCommand.class);

    @Inject
    public GetNewAddressCommand() {}

    @Override
    public Result execute(CommandContext context) {
        Result result = new DefaultResult();
        try {
            Integer count = context.getParameter("count");
            if (count == null || count < 1) {
                count = 1;
            }

            GetNewAddressReply reply = context
                    .adminService()
                    .getNewAddress(GetNewAddressRequest.newBuilder()
                            .setCount(count)
                            .build());
            if (!reply.getSuccess()) {
                result.fail();
            } else {
                FormattableObject<NewAddressPayload> temp = new FormattableObject<>(reply.getResultsList());
                temp.success = !result.didFail();
                temp.payload = new NewAddressPayload(reply);

                context.outputObject(temp);

                context.suggestCommands(Arrays.asList(
                        BackupWalletCommand.class,
                        GetBalanceCommand.class,
                        GetHistoryCommand.class,
                        SigIndexCommand.class
                ));

            }
            for (nodecore.api.grpc.Result r : reply.getResultsList())
                result.addMessage(r.getCode(), r.getMessage(), r.getDetails(), r.getError());
        } catch (StatusRuntimeException e) {
            CommandUtility.handleRuntimeException(result, e, _logger);
        }

        return result;
    }
}
