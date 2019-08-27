// VeriBlock NodeCore
// Copyright 2017-2019 Xenios SEZC
// All rights reserved.
// https://www.veriblock.org
// Distributed under the MIT software license, see the accompanying
// file LICENSE or http://www.opensource.org/licenses/mit-license.php.

package nodecore.cli.commands.rpc;

import com.google.inject.Inject;
import io.grpc.StatusRuntimeException;
import nodecore.api.grpc.DrainAddressReply;
import nodecore.api.grpc.DrainAddressRequest;
import nodecore.api.grpc.utilities.ByteStringAddressUtility;
import nodecore.cli.annotations.CommandParameterType;
import nodecore.cli.annotations.CommandSpec;
import nodecore.cli.annotations.CommandSpecParameter;
import nodecore.cli.commands.serialization.FormattableObject;
import nodecore.cli.commands.serialization.TransactionInfo;
import nodecore.cli.contracts.Command;
import nodecore.cli.contracts.CommandContext;
import nodecore.cli.contracts.DefaultResult;
import nodecore.cli.contracts.Result;
import nodecore.cli.utilities.CommandUtility;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


@CommandSpec(
        name = "Drain Address",
        form = "drainaddress",
        description = "Transfers the entire balance of coins for an address")
@CommandSpecParameter(name = "sourceAddress", required = true, type = CommandParameterType.STANDARD_ADDRESS)
@CommandSpecParameter(name = "destinationAddress", required = true, type = CommandParameterType.STANDARD_OR_MULTISIG_ADDRESS)
public class DrainAddressCommand implements Command {
    private static final Logger logger = LoggerFactory.getLogger(DrainAddressCommand.class);

    @Inject
    public DrainAddressCommand() {}

    @Override
    public Result execute(CommandContext context) throws Exception {
        Result result = new DefaultResult();

        String sourceAddress = context.getParameter("sourceAddress");
        String destinationAddress = context.getParameter("destinationAddress");

        try {
            DrainAddressReply reply = context.adminService().drainAddress(DrainAddressRequest.newBuilder()
                    .setSourceAddress(ByteStringAddressUtility.createProperByteStringAutomatically(sourceAddress))
                    .setDestinationAddress(ByteStringAddressUtility.createProperByteStringAutomatically(destinationAddress))
                    .build());
            if (!reply.getSuccess()) {
                result.fail();
            } else {
                FormattableObject<TransactionInfo> temp = new FormattableObject<>(reply.getResultsList());
                temp.success = !result.didFail();
                temp.payload = new TransactionInfo(reply.getTransaction());

                context.outputObject(temp);

            }
            for (nodecore.api.grpc.Result r : reply.getResultsList())
                result.addMessage(r.getCode(), r.getMessage(), r.getDetails(), r.getError());
        } catch (StatusRuntimeException e) {
            CommandUtility.handleRuntimeException(result, e, logger);
        }

        return result;
    }
}
