// VeriBlock NodeCore CLI
// Copyright 2017-2019 Xenios SEZC
// All rights reserved.
// https://www.veriblock.org
// Distributed under the MIT software license, see the accompanying
// file LICENSE or http://www.opensource.org/licenses/mit-license.php.

package nodecore.cli.commands.rpc;

import com.google.inject.Inject;
import io.grpc.StatusRuntimeException;
import nodecore.api.grpc.Output;
import nodecore.api.grpc.SendCoinsReply;
import nodecore.api.grpc.SendCoinsRequest;
import nodecore.api.grpc.utilities.ByteStringAddressUtility;
import nodecore.cli.annotations.CommandParameterType;
import nodecore.cli.annotations.CommandSpec;
import nodecore.cli.annotations.CommandSpecParameter;
import nodecore.cli.commands.serialization.FormattableObject;
import nodecore.cli.commands.serialization.SendCoinsPayload;
import nodecore.cli.contracts.Command;
import nodecore.cli.contracts.CommandContext;
import nodecore.cli.contracts.DefaultResult;
import nodecore.cli.contracts.Result;
import nodecore.cli.utilities.CommandUtility;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.veriblock.core.utilities.AddressUtility;

import java.util.Arrays;

@CommandSpec(
        name = "Send",
        form = "send",
        description = "Send coins to the specified address")
@CommandSpecParameter(name = "amount", required = true, type = CommandParameterType.STRING)
@CommandSpecParameter(name = "destinationAddress", required = true, type = CommandParameterType.STANDARD_OR_MULTISIG_ADDRESS)
@CommandSpecParameter(name = "sourceAddress", required = false, type = CommandParameterType.STANDARD_ADDRESS)
public class SendCommand implements Command {
    private static final Logger _logger = LoggerFactory.getLogger(SendCommand.class);

    @Inject
    public SendCommand() {}

    @Override
    public Result execute(CommandContext context) {
        Result result = new DefaultResult();
        String amount = context.getParameter("amount");
        long atomicAmount = org.veriblock.core.utilities.Utility.convertDecimalCoinToAtomicLong(amount);
        String destinationAddress = context.getParameter("destinationAddress");
        String sourceAddress = context.getParameter("sourceAddress");

        try {
            SendCoinsRequest.Builder request = SendCoinsRequest.newBuilder();
            if (AddressUtility.isValidStandardOrMultisigAddress(destinationAddress)) {
                request.addAmounts(Output.newBuilder()
                        .setAddress(ByteStringAddressUtility.createProperByteStringAutomatically(destinationAddress))
                        .setAmount(atomicAmount));
            } else {
                // Should never happen; address validity is checked by argument parser
                result.fail();
                return null;
            }

            if (sourceAddress != null && AddressUtility.isValidStandardAddress(sourceAddress)) {
                request.setSourceAddress(ByteStringAddressUtility.createProperByteStringAutomatically(sourceAddress));
            }

            SendCoinsReply reply = context
                    .adminService()
                    .sendCoins(request.build());
            if (!reply.getSuccess()) {
                result.fail();
            } else {
                FormattableObject<SendCoinsPayload> temp = new FormattableObject<>(reply.getResultsList());
                temp.success = !result.didFail();
                temp.payload = new SendCoinsPayload(reply);

                context.outputObject(temp);

                context.suggestCommands(Arrays.asList(
                        GetHistoryCommand.class,
                        GetBalanceCommand.class
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
