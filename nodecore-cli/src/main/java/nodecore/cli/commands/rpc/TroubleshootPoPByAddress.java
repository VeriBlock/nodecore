// VeriBlock NodeCore CLI
// Copyright 2017-2019 Xenios SEZC
// All rights reserved.
// https://www.veriblock.org
// Distributed under the MIT software license, see the accompanying
// file LICENSE or http://www.opensource.org/licenses/mit-license.php.

package nodecore.cli.commands.rpc;

import com.google.inject.Inject;
import io.grpc.StatusRuntimeException;
import nodecore.api.grpc.AddressSet;
import nodecore.api.grpc.TroubleshootPoPTransactionsReply;
import nodecore.api.grpc.TroubleshootPoPTransactionsRequest;
import nodecore.api.grpc.utilities.ByteStringAddressUtility;
import nodecore.cli.annotations.CommandParameterType;
import nodecore.cli.annotations.CommandSpec;
import nodecore.cli.annotations.CommandSpecParameter;
import nodecore.cli.commands.serialization.FormattableObject;
import nodecore.cli.commands.serialization.TroubleshootPoPTransactionsPayload;
import nodecore.cli.contracts.*;
import nodecore.cli.utilities.CommandUtility;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Arrays;

@CommandSpec(
        name = "Troubleshoot PoP By Address",
        form = "troubleshootpopbyaddress",
        description = "(Returns a troubleshooting report of the PoP transaction(s) matching the provided address in the specified history)")
@CommandSpecParameter(name = "onlyFailures", required = true, type = CommandParameterType.BOOLEAN)
@CommandSpecParameter(name = "searchLength", required = false, type = CommandParameterType.INTEGER)
@CommandSpecParameter(name = "address", required = false, type = CommandParameterType.STANDARD_ADDRESS)
public class TroubleshootPoPByAddress implements Command {
    private static final Logger _logger = LoggerFactory.getLogger(ValidateAddressCommand.class);
    private Configuration _configuration;

    @Inject
    public TroubleshootPoPByAddress(Configuration configuration) {
        _configuration = configuration;
    }

    @Override
    public Result execute(CommandContext context) {
        Result result = new DefaultResult();
        try {
            TroubleshootPoPTransactionsRequest.Builder requestBuilder = TroubleshootPoPTransactionsRequest.newBuilder();

            String address = context.getParameter("address");
            if (address != null) {
                requestBuilder.setAddresses(AddressSet.newBuilder()
                        .addAddresses(ByteStringAddressUtility.createProperByteStringAutomatically(address)));
            }

            requestBuilder.setOnlyFailures(context.getParameter("onlyFailures"));

            Integer searchLength = context.getParameter("searchLength");

            if (searchLength != null) {
                requestBuilder.setSearchLength(searchLength);
            } else {
                requestBuilder.setSearchLength(2000);
            }

            TroubleshootPoPTransactionsReply reply = context
                    .adminService()
                    .troubleshootPoPTransactions(requestBuilder.build());
            if (!reply.getSuccess()) {
                result.fail();
            } else {
                FormattableObject<TroubleshootPoPTransactionsPayload> temp = new FormattableObject<>(reply.getResultsList());
                temp.success = !result.didFail();
                temp.payload = new TroubleshootPoPTransactionsPayload(reply);

                context.outputObject(temp);

                context.suggestCommands(Arrays.asList(
                        GetBalanceCommand.class,
                        GetHistoryCommand.class));
            }
            for (nodecore.api.grpc.Result r : reply.getResultsList())
                result.addMessage(r.getCode(), r.getMessage(), r.getDetails(), r.getError());
        } catch (StatusRuntimeException e) {
            CommandUtility.handleRuntimeException(result, e, _logger);
        }

        return result;
    }
}
