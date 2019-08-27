// VeriBlock NodeCore CLI
// Copyright 2017-2019 Xenios SEZC
// All rights reserved.
// https://www.veriblock.org
// Distributed under the MIT software license, see the accompanying
// file LICENSE or http://www.opensource.org/licenses/mit-license.php.

package nodecore.cli.commands.rpc;

import com.google.inject.Inject;
import io.grpc.StatusRuntimeException;
import nodecore.api.grpc.GetSignatureIndexReply;
import nodecore.api.grpc.GetSignatureIndexRequest;
import nodecore.api.grpc.utilities.ByteStringAddressUtility;
import nodecore.cli.annotations.CommandParameterType;
import nodecore.cli.annotations.CommandSpec;
import nodecore.cli.annotations.CommandSpecParameter;
import nodecore.cli.commands.serialization.FormattableObject;
import nodecore.cli.commands.serialization.SignatureIndexPayload;
import nodecore.cli.contracts.Command;
import nodecore.cli.contracts.CommandContext;
import nodecore.cli.contracts.DefaultResult;
import nodecore.cli.contracts.Result;
import nodecore.cli.utilities.CommandUtility;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Arrays;

@CommandSpec(
        name = "Signature Index",
        form = "sigindex",
        description = "Gets the signature index for the specified address")
@CommandSpecParameter(name = "address", required = false, type = CommandParameterType.STANDARD_OR_MULTISIG_ADDRESS)
public class SigIndexCommand implements Command {
    private static final Logger _logger = LoggerFactory.getLogger(SigIndexCommand.class);

    @Inject
    public SigIndexCommand() {
    }

    @Override
    public Result execute(CommandContext context) {
        Result result = new DefaultResult();

        String address = context.getParameter("address");
        try {
            GetSignatureIndexRequest.Builder requestBuilder = GetSignatureIndexRequest.newBuilder();
            if (address != null) {
                requestBuilder.addAddresses(ByteStringAddressUtility.createProperByteStringAutomatically(address));
            }
            GetSignatureIndexReply reply = context
                    .adminService()
                    .getSignatureIndex(requestBuilder.build());
            if (!reply.getSuccess()) {
                result.fail();
            } else {
                FormattableObject<SignatureIndexPayload> temp = new FormattableObject<>(reply.getResultsList());
                temp.success = !result.didFail();
                temp.payload = new SignatureIndexPayload(reply);

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
