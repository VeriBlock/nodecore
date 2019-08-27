// VeriBlock NodeCore
// Copyright 2017-2019 Xenios SEZC
// All rights reserved.
// https://www.veriblock.org
// Distributed under the MIT software license, see the accompanying
// file LICENSE or http://www.opensource.org/licenses/mit-license.php.

package nodecore.cli.commands.rpc;

import com.google.inject.Inject;
import io.grpc.StatusRuntimeException;
import nodecore.api.grpc.AddressBalanceSchedule;
import nodecore.api.grpc.GetBalanceUnlockScheduleReply;
import nodecore.api.grpc.GetBalanceUnlockScheduleRequest;
import nodecore.api.grpc.utilities.ByteStringAddressUtility;
import nodecore.cli.annotations.CommandParameterType;
import nodecore.cli.annotations.CommandSpec;
import nodecore.cli.annotations.CommandSpecParameter;
import nodecore.cli.commands.serialization.AddressBalanceSchedulePayload;
import nodecore.cli.commands.serialization.FormattableObject;
import nodecore.cli.contracts.Command;
import nodecore.cli.contracts.CommandContext;
import nodecore.cli.contracts.DefaultResult;
import nodecore.cli.contracts.Result;
import nodecore.cli.utilities.CommandUtility;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@CommandSpec(
        name = "Get Balance Unlock Schedule",
        form = "getbalanceunlockschedule",
        description = "See the schedule in which locked balance become available")
@CommandSpecParameter(name = "address", required = false, type = CommandParameterType.STANDARD_OR_MULTISIG_ADDRESS)
public class GetBalanceUnlockScheduleCommand implements Command {
    private static final Logger logger = LoggerFactory.getLogger(GetBalanceUnlockScheduleCommand.class);

    @Inject
    public GetBalanceUnlockScheduleCommand() {}

    @Override
    public Result execute(CommandContext context) throws Exception {
        Result result = new DefaultResult();

        String address = context.getParameter("address");
        try {
            GetBalanceUnlockScheduleRequest.Builder requestBuilder = GetBalanceUnlockScheduleRequest.newBuilder();
            if (address != null) {
                requestBuilder.addAddresses(ByteStringAddressUtility.createProperByteStringAutomatically(address));
            }

            GetBalanceUnlockScheduleReply reply = context
                    .adminService()
                    .getBalanceUnlockSchedule(requestBuilder.build());
            if (!reply.getSuccess()) {
                result.fail();
            } else {
                FormattableObject<List<AddressBalanceSchedulePayload>> temp = new FormattableObject<>(reply.getResultsList());
                temp.success = !result.didFail();

                List<AddressBalanceSchedulePayload> payload = new ArrayList<>();
                for (AddressBalanceSchedule sched : reply.getAddressScheduleList()) {
                    payload.add(new AddressBalanceSchedulePayload(sched));
                }
                temp.payload = payload;

                context.outputObject(temp);

                context.suggestCommands(Collections.singletonList(GetBalanceCommand.class));

            }
            for (nodecore.api.grpc.Result r : reply.getResultsList())
                result.addMessage(r.getCode(), r.getMessage(), r.getDetails(), r.getError());
        } catch (StatusRuntimeException e) {
            CommandUtility.handleRuntimeException(result, e, logger);

        }

        return result;
    }
}
