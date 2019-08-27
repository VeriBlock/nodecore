// VeriBlock NodeCore CLI
// Copyright 2017-2019 Xenios SEZC
// All rights reserved.
// https://www.veriblock.org
// Distributed under the MIT software license, see the accompanying
// file LICENSE or http://www.opensource.org/licenses/mit-license.php.

package nodecore.cli.commands.rpc;

import com.google.inject.Inject;
import io.grpc.StatusRuntimeException;
import nodecore.api.grpc.SetDefaultAddressReply;
import nodecore.api.grpc.SetDefaultAddressRequest;
import nodecore.api.grpc.utilities.ByteStringAddressUtility;
import nodecore.cli.annotations.CommandParameterType;
import nodecore.cli.annotations.CommandSpec;
import nodecore.cli.annotations.CommandSpecParameter;
import nodecore.cli.commands.serialization.EmptyPayload;
import nodecore.cli.commands.serialization.FormattableObject;
import nodecore.cli.contracts.Command;
import nodecore.cli.contracts.CommandContext;
import nodecore.cli.contracts.DefaultResult;
import nodecore.cli.contracts.Result;
import nodecore.cli.utilities.CommandUtility;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Arrays;

@CommandSpec(
        name = "Set Default Address",
        form = "setdefaultaddress",
        description = "Sets the default address of a NodeCore instance")

@CommandSpecParameter(name = "address", required = true, type = CommandParameterType.STANDARD_ADDRESS)
public class SetDefaultAddressCommand implements Command {
    private static final Logger _logger = LoggerFactory.getLogger(SetDefaultAddressCommand.class);

    @Inject
    public SetDefaultAddressCommand() {
    }

    @Override
    public Result execute(CommandContext context) {
        Result result = new DefaultResult();

        try {
            String address = context.getParameter("address");

            SetDefaultAddressReply reply = context
                    .adminService()
                    .setDefaultAddress(SetDefaultAddressRequest.newBuilder()
                            .setAddress(ByteStringAddressUtility.createProperByteStringAutomatically(address))
                            .build());
            if (!reply.getSuccess()) {
                result.fail();
            } else {
                FormattableObject<EmptyPayload> temp = new FormattableObject<>(reply.getResultsList());
                temp.success = !result.didFail();
                temp.payload = new EmptyPayload();

                context.outputObject(temp);

                context.suggestCommands(Arrays.asList(
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
