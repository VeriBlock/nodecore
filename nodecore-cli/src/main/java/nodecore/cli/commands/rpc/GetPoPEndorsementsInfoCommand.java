// VeriBlock NodeCore
// Copyright 2017-2019 Xenios SEZC
// All rights reserved.
// https://www.veriblock.org
// Distributed under the MIT software license, see the accompanying
// file LICENSE or http://www.opensource.org/licenses/mit-license.php.


// VeriBlock NodeCore CLI
// Copyright 2017-2019 Xenios SEZC
// All rights reserved.
// https://www.veriblock.org
// Distributed under the MIT software license, see the accompanying
// file LICENSE or http://www.opensource.org/licenses/mit-license.php.

package nodecore.cli.commands.rpc;

import com.google.inject.Inject;
import io.grpc.StatusRuntimeException;
import nodecore.api.grpc.GetPoPEndorsementsInfoReply;
import nodecore.api.grpc.GetPoPEndorsementsInfoRequest;
import nodecore.api.grpc.StandardAddress;
import nodecore.api.grpc.utilities.ByteStringAddressUtility;
import nodecore.cli.annotations.CommandParameterType;
import nodecore.cli.annotations.CommandSpec;
import nodecore.cli.annotations.CommandSpecParameter;
import nodecore.cli.commands.serialization.FormattableObject;
import nodecore.cli.commands.serialization.PoPEndorsementsInfo;
import nodecore.cli.commands.shell.StartPoPMinerCommand;
import nodecore.cli.contracts.Command;
import nodecore.cli.contracts.CommandContext;
import nodecore.cli.contracts.DefaultResult;
import nodecore.cli.contracts.Result;
import nodecore.cli.utilities.CommandUtility;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

@CommandSpec(
        name = "Get PoP Endorsements Info",
        form = "getpopendorsementsinfo",
        description = "Returns the PoP endorsements related to a particular address given the particular search length")

@CommandSpecParameter(name = "address", required = true, type = CommandParameterType.STANDARD_ADDRESS)
@CommandSpecParameter(name = "searchLength", required = false, type = CommandParameterType.INTEGER)
public class GetPoPEndorsementsInfoCommand implements Command {
    private static final Logger _logger = LoggerFactory.getLogger(GetPoPEndorsementsInfoCommand.class);

    @Inject
    public GetPoPEndorsementsInfoCommand() {
    }

    @Override
    public Result execute(CommandContext context) {
        Result result = new DefaultResult();

        String address = context.getParameter("address");
        Integer searchLength = context.getParameter("searchLength");

        try {
            GetPoPEndorsementsInfoRequest.Builder requestBuilder = GetPoPEndorsementsInfoRequest.newBuilder();
            requestBuilder.addAddresses(StandardAddress.newBuilder()
                    .setStandardAddress(ByteStringAddressUtility.createProperByteStringAutomatically(address)));

            if (searchLength != null)
                requestBuilder.setSearchLength(searchLength);

            GetPoPEndorsementsInfoReply reply = context
                    .adminService()
                    .getPoPEndorsementsInfo(requestBuilder.build());
            if (!reply.getSuccess()) {
                result.fail();
            } else {
                FormattableObject<List<PoPEndorsementsInfo>> temp = new FormattableObject<>(reply.getResultsList());

                _logger.info("We received a total of " + reply.getPopEndorsementsCount() + " pop endorsements!");

                temp.success = !result.didFail();
                temp.payload = reply.getPopEndorsementsList()
                        .stream()
                        .map(PoPEndorsementsInfo::new).collect(Collectors.toList());

                context.outputObject(temp);

                context.suggestCommands(Arrays.asList(
                        GetProtectedChildrenCommand.class,
                        GetProtectingParentsCommand.class,
                        StartPoPMinerCommand.class
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
