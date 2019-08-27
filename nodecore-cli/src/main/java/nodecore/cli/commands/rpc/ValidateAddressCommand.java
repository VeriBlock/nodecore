// VeriBlock NodeCore CLI
// Copyright 2017-2019 Xenios SEZC
// All rights reserved.
// https://www.veriblock.org
// Distributed under the MIT software license, see the accompanying
// file LICENSE or http://www.opensource.org/licenses/mit-license.php.

package nodecore.cli.commands.rpc;

import com.google.inject.Inject;
import io.grpc.StatusRuntimeException;
import nodecore.api.grpc.ValidateAddressReply;
import nodecore.api.grpc.ValidateAddressRequest;
import nodecore.api.grpc.utilities.ByteStringAddressUtility;
import nodecore.cli.annotations.CommandParameterType;
import nodecore.cli.annotations.CommandSpec;
import nodecore.cli.annotations.CommandSpecParameter;
import nodecore.cli.commands.serialization.FormattableObject;
import nodecore.cli.commands.serialization.ValidateAddressPayload;
import nodecore.cli.contracts.Command;
import nodecore.cli.contracts.CommandContext;
import nodecore.cli.contracts.DefaultResult;
import nodecore.cli.contracts.Result;
import nodecore.cli.utilities.CommandUtility;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Arrays;

@CommandSpec(
        name = "Validate Address",
        form = "validateaddress",
        description = "Returns details about an address if it is valid")
@CommandSpecParameter(name = "address", required = true, type = CommandParameterType.STANDARD_ADDRESS)
public class ValidateAddressCommand implements Command {
    private static final Logger _logger = LoggerFactory.getLogger(ValidateAddressCommand.class);

    @Inject
    public ValidateAddressCommand() {}

    @Override
    public Result execute(CommandContext context) {
        Result result = new DefaultResult();
        try {
            ValidateAddressRequest.Builder requestBuilder = ValidateAddressRequest.newBuilder();
            requestBuilder.setAddress(ByteStringAddressUtility.createProperByteStringAutomatically(context.getParameter("address")));
            ValidateAddressReply reply = context
                    .adminService()
                    .validateAddress(requestBuilder.build());
            if (!reply.getSuccess()) {
                result.fail();
            } else {
                FormattableObject<ValidateAddressPayload> temp = new FormattableObject<>(reply.getResultsList());
                temp.success = !result.didFail();
                temp.payload = new ValidateAddressPayload(reply);
                context.outputObject(temp);

                context.suggestCommands(Arrays.asList(
                        GetBalanceCommand.class,
                        GetHistoryCommand.class
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
