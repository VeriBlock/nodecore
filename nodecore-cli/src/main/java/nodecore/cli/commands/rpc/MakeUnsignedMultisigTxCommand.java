// VeriBlock NodeCore CLI
// Copyright 2017-2019 Xenios SEZC
// All rights reserved.
// https://www.veriblock.org
// Distributed under the MIT software license, see the accompanying
// file LICENSE or http://www.opensource.org/licenses/mit-license.php.

package nodecore.cli.commands.rpc;

import com.google.inject.Inject;
import com.google.protobuf.ByteString;
import io.grpc.StatusRuntimeException;
import nodecore.api.grpc.MakeUnsignedMultisigTxReply;
import nodecore.api.grpc.MakeUnsignedMultisigTxRequest;
import nodecore.api.grpc.utilities.ByteStringAddressUtility;
import nodecore.cli.annotations.CommandParameterType;
import nodecore.cli.annotations.CommandSpec;
import nodecore.cli.annotations.CommandSpecParameter;
import nodecore.cli.commands.serialization.FormattableObject;
import nodecore.cli.commands.serialization.MakeUnsignedMultisigTxPayload;
import nodecore.cli.contracts.*;
import nodecore.cli.utilities.CommandUtility;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.veriblock.core.utilities.Utility;

import java.util.Arrays;

@CommandSpec(
        name = "Make Unsigned Multisig Tx",
        form = "makeunsignedmultisigtx",
        description = "(Generates an unsigned multisig transaction)")
@CommandSpecParameter(name = "sourceAddress", required = true, type = CommandParameterType.MULTISIG_ADDRESS)
@CommandSpecParameter(name = "amount", required = true, type = CommandParameterType.STRING)
@CommandSpecParameter(name = "destinationAddress", required = true, type = CommandParameterType.STANDARD_OR_MULTISIG_ADDRESS)
@CommandSpecParameter(name = "transactionFee", required = false, type = CommandParameterType.STRING)
@CommandSpecParameter(name = "signatureIndex", required = false, type = CommandParameterType.INTEGER)
public class MakeUnsignedMultisigTxCommand implements Command {
    private static final Logger _logger = LoggerFactory.getLogger(ValidateAddressCommand.class);
    private Configuration _configuration;

    @Inject
    public MakeUnsignedMultisigTxCommand(Configuration configuration) {
        _configuration = configuration;
    }

    @Override
    public Result execute(CommandContext context) {
        Result result = new DefaultResult();
        try {
            String amount = context.getParameter("amount");
            long atomicAmount = Utility.convertDecimalCoinToAtomicLong(amount);
            String destinationAddress = context.getParameter("destinationAddress");
            String sourceAddress = context.getParameter("sourceAddress");

            MakeUnsignedMultisigTxRequest.Builder requestBuilder = MakeUnsignedMultisigTxRequest.newBuilder();

            requestBuilder.setSourceMultisigAddress(ByteStringAddressUtility.createProperByteStringAutomatically(sourceAddress));

            requestBuilder.addAmounts(nodecore.api.grpc.Output.newBuilder()
                    .setAddress(ByteStringAddressUtility.createProperByteStringAutomatically(destinationAddress))
                    .setAmount(atomicAmount));

            String transactionFee = context.getParameter("transactionFee");
            if (transactionFee != null) {
                requestBuilder.setFee(Utility.convertDecimalCoinToAtomicLong(transactionFee));
            }

            Integer signatureIndex = context.getParameter("signatureIndex");
            if (signatureIndex != null) {
                requestBuilder.setSignatureIndexString(ByteString.copyFrom(("" + signatureIndex).getBytes()));
            }

            MakeUnsignedMultisigTxReply reply = context
                    .adminService()
                    .makeUnsignedMultisigTx(requestBuilder.build());


            if (!reply.getSuccess()) {
                result.fail();
            } else {
                FormattableObject<MakeUnsignedMultisigTxPayload> temp = new FormattableObject<>(reply.getResultsList());
                temp.success = !result.didFail();
                temp.payload = new MakeUnsignedMultisigTxPayload(reply);
                context.outputObject(temp);

                context.suggestCommands(Arrays.asList(
                        GetBalanceCommand.class,
                        GetHistoryCommand.class,
                        GenerateMultisigAddressCommand.class,
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
