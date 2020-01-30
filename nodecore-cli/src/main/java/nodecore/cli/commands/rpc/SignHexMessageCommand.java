// VeriBlock NodeCore CLI
// Copyright 2017-2019 Xenios SEZC
// All rights reserved.
// https://www.veriblock.org
// Distributed under the MIT software license, see the accompanying
// file LICENSE or http://www.opensource.org/licenses/mit-license.php.

package nodecore.cli.commands.rpc;

import com.google.protobuf.ByteString;
import io.grpc.StatusRuntimeException;
import nodecore.api.grpc.VeriBlockMessages;
import nodecore.api.grpc.utilities.ByteStringAddressUtility;
import nodecore.cli.annotations.CommandParameterType;
import nodecore.cli.annotations.CommandSpec;
import nodecore.cli.annotations.CommandSpecParameter;
import nodecore.cli.commands.serialization.FormattableObject;
import nodecore.cli.commands.serialization.SignMessagePayload;
import nodecore.cli.contracts.Command;
import nodecore.cli.contracts.CommandContext;
import nodecore.cli.utilities.CommandUtility;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.veriblock.core.utilities.Utility;

@CommandSpec(
        name = "Sign Hex Message",
        form = "signhexmessage",
        description = "(Signs a hex-encoded message with the addresses private key)")
@CommandSpecParameter(name = "address", required = true, type = CommandParameterType.STANDARD_ADDRESS)
@CommandSpecParameter(name = "message", required = true, type = CommandParameterType.HEXSTRING)
public class SignHexMessageCommand implements Command {
    private static final Logger _logger = LoggerFactory.getLogger(SignMessageCommand.class);
    private Configuration _configuration;

    public SignHexMessageCommand(Configuration configuration) {
        _configuration = configuration;
    }

    @Override
    public Result execute(CommandContext context) {
        Result result = new DefaultResult();
        try {
            VeriBlockMessages.SignMessageRequest.Builder requestBuilder = VeriBlockMessages.SignMessageRequest.newBuilder();

            String address = context.getParameter("address");
            byte[] message = Utility.hexToBytes(context.getParameter("message"));

            requestBuilder.setAddress(ByteStringAddressUtility.createProperByteStringAutomatically(address));

            requestBuilder.setMessage(ByteString.copyFrom(message));

            VeriBlockMessages.SignMessageReply reply = context
                    .adminService()
                    .signMessage(requestBuilder.build());
            if (!reply.getSuccess()) {
                result.fail();
            } else {
                FormattableObject<SignMessagePayload> temp = new FormattableObject<>(reply.getResultsList());
                temp.success = !result.didFail();
                temp.payload = new SignMessagePayload(address, reply);
                context.outputObject(temp);
            }
            for (VeriBlockMessages.Result r : reply.getResultsList())
                result.addMessage(r.getCode(), r.getMessage(), r.getDetails(), r.getError());
        } catch (StatusRuntimeException e) {
            CommandUtility.handleRuntimeException(result, e, _logger);
        }

        return result;
    }
}
