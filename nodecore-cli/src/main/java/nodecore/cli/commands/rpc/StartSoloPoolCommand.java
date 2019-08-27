// VeriBlock NodeCore CLI
// Copyright 2017-2019 Xenios SEZC
// All rights reserved.
// https://www.veriblock.org
// Distributed under the MIT software license, see the accompanying
// file LICENSE or http://www.opensource.org/licenses/mit-license.php.

package nodecore.cli.commands.rpc;

import com.google.inject.Inject;
import io.grpc.StatusRuntimeException;
import nodecore.api.grpc.StartSoloPoolReply;
import nodecore.api.grpc.StartSoloPoolRequest;
import nodecore.api.grpc.utilities.ByteStringAddressUtility;
import nodecore.cli.annotations.CommandParameterType;
import nodecore.cli.annotations.CommandSpec;
import nodecore.cli.annotations.CommandSpecParameter;
import nodecore.cli.commands.serialization.EmptyPayload;
import nodecore.cli.commands.serialization.FormattableObject;
import nodecore.cli.contracts.Command;
import nodecore.cli.contracts.CommandContext;
import nodecore.cli.contracts.DefaultResult;
import nodecore.cli.contracts.Result;
import nodecore.cli.utilities.CommandUtility;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Arrays;

@CommandSpec(
        name = "Start Solo Pool",
        form = "startsolopool",
        description = "Starts the built-in pool service in NodeCore in solo mode")
@CommandSpecParameter(name = "address", required = false, type = CommandParameterType.STANDARD_ADDRESS)
public class StartSoloPoolCommand implements Command {
    private static final Logger _logger = LoggerFactory.getLogger(StartSoloPoolCommand.class);

    @Inject
    public StartSoloPoolCommand() {
    }

    @Override
    public Result execute(CommandContext context) {
        Result result = new DefaultResult();
        try {
            String address = context.getParameter("address");
            StartSoloPoolReply reply;

            if (address != null) {
                reply = context
                        .adminService()
                        .startSoloPool(StartSoloPoolRequest.newBuilder()
                                .setAddress(ByteStringAddressUtility.createProperByteStringAutomatically(address))
                                .build());
            } else {
                reply = context
                        .adminService()
                        .startSoloPool(StartSoloPoolRequest.newBuilder().build());
            }

            if (!reply.getSuccess()) {
                result.fail();
            } else {
                FormattableObject<EmptyPayload> temp = new FormattableObject<>(reply.getResultsList());
                temp.success = !result.didFail();
                temp.payload = new EmptyPayload();

                context.outputObject(temp);

                String infoMessage = "By default, your pool homepage will be available in a web browser at:\n" +
                        "\thttp://127.0.0.1:8500\n" +
                        "And a VeriBlock Proof-of-Work (PoW) miner can be pointed to:\n" +
                        "\t127.0.0.1:8501\n" +
                        "Remember that by default a solo pool mines to your default address!\n" +
                        "You can view your default address with the command: " +
                        "getinfo";

                context.write().info(infoMessage);

                context.suggestCommands(Arrays.asList(
                        StopPoolCommand.class,
                        GetBalanceCommand.class,
                        SetDefaultAddressCommand.class
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
