// VeriBlock NodeCore CLI
// Copyright 2017-2019 Xenios SEZC
// All rights reserved.
// https://www.veriblock.org
// Distributed under the MIT software license, see the accompanying
// file LICENSE or http://www.opensource.org/licenses/mit-license.php.

package nodecore.cli.commands.rpc;

import com.google.inject.Inject;
import io.grpc.StatusRuntimeException;
import nodecore.api.grpc.GenerateMultisigAddressReply;
import nodecore.api.grpc.GenerateMultisigAddressRequest;
import nodecore.api.grpc.utilities.ByteStringAddressUtility;
import nodecore.cli.annotations.CommandParameterType;
import nodecore.cli.annotations.CommandSpec;
import nodecore.cli.annotations.CommandSpecParameter;
import nodecore.cli.commands.serialization.FormattableObject;
import nodecore.cli.commands.serialization.GenerateMultisigAddressPayload;
import nodecore.cli.contracts.*;
import nodecore.cli.utilities.CommandUtility;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Arrays;

@CommandSpec(
        name = "Generate Multisig Address",
        form = "generatemultisigaddress",
        description = "(Generates a multisig address from the provided standard addresses)")
@CommandSpecParameter(name = "csvaddresses", required = true, type = CommandParameterType.COMMA_SEPARATED_STANDARD_ADDRESSES)
@CommandSpecParameter(name = "signatureThreshold", required = true, type = CommandParameterType.INTEGER)
public class GenerateMultisigAddressCommand implements Command {
    private static final Logger _logger = LoggerFactory.getLogger(ValidateAddressCommand.class);
    private Configuration _configuration;

    @Inject
    public GenerateMultisigAddressCommand(Configuration configuration) {
        _configuration = configuration;
    }

    @Override
    public Result execute(CommandContext context) {
        Result result = new DefaultResult();
        try {
            GenerateMultisigAddressRequest.Builder requestBuilder = GenerateMultisigAddressRequest.newBuilder();

            int signatureThreshold = context.getParameter("signatureThreshold");
            requestBuilder.setSignatureThresholdM(signatureThreshold);

            String[] addresses = context.getParameter("csvaddresses").toString().split(",");
            for (String address : addresses) {
                requestBuilder.addSourceAddresses(ByteStringAddressUtility.createProperByteStringAutomatically(address));
            }

            GenerateMultisigAddressReply reply = context
                    .adminService()
                    .generateMultisigAddress(requestBuilder.build());
            if (!reply.getSuccess()) {
                result.fail();
            } else {
                FormattableObject<GenerateMultisigAddressPayload> temp = new FormattableObject<>(reply.getResultsList());
                temp.success = !result.didFail();
                temp.payload = new GenerateMultisigAddressPayload(reply);

                context.outputObject(temp);

                context.suggestCommands(Arrays.asList(
                        GetBalanceCommand.class,
                        GetHistoryCommand.class,
                        MakeUnsignedMultisigTxCommand.class,
                        SubmitMultisigTxCommand.class
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
