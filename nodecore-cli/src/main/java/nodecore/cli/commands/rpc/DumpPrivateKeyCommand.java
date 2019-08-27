// VeriBlock NodeCore CLI
// Copyright 2017-2019 Xenios SEZC
// All rights reserved.
// https://www.veriblock.org
// Distributed under the MIT software license, see the accompanying
// file LICENSE or http://www.opensource.org/licenses/mit-license.php.

package nodecore.cli.commands.rpc;

import com.google.inject.Inject;
import io.grpc.StatusRuntimeException;
import nodecore.api.grpc.DumpPrivateKeyReply;
import nodecore.api.grpc.DumpPrivateKeyRequest;
import nodecore.api.grpc.utilities.ByteStringAddressUtility;
import nodecore.cli.annotations.CommandParameterType;
import nodecore.cli.annotations.CommandSpec;
import nodecore.cli.annotations.CommandSpecParameter;
import nodecore.cli.commands.serialization.FormattableObject;
import nodecore.cli.commands.serialization.PrivateKeyInfo;
import nodecore.cli.contracts.Command;
import nodecore.cli.contracts.CommandContext;
import nodecore.cli.contracts.DefaultResult;
import nodecore.cli.contracts.Result;
import nodecore.cli.utilities.CommandUtility;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Arrays;

@CommandSpec(
        name = "Dump Private Key",
        form = "dumpprivatekey",
        description = "Gets private key for an address")

@CommandSpecParameter(name = "address", required = true, type = CommandParameterType.STANDARD_ADDRESS)
public class DumpPrivateKeyCommand implements Command {
    private static final Logger _logger = LoggerFactory.getLogger(DumpPrivateKeyCommand.class);

    @Inject
    public DumpPrivateKeyCommand() {
    }

    @Override
    public Result execute(CommandContext context) {
        Result result = new DefaultResult();

        try {
            String address = context.getParameter("address");
            DumpPrivateKeyReply reply;

            reply = context
                    .adminService()
                    .dumpPrivateKey(DumpPrivateKeyRequest.newBuilder()
                            .setAddress(ByteStringAddressUtility.createProperByteStringAutomatically(address))
                            .build());

            if (!reply.getSuccess()) {
                result.fail();
            } else {
                FormattableObject<PrivateKeyInfo> temp = new FormattableObject<>(reply.getResultsList());
                temp.success = !result.didFail();
                temp.payload = new PrivateKeyInfo(reply);

                context.outputObject(temp);

                context.write().warning(String.format("Anyone with access to this private key can steal any funds held in %s!" +
                                "Make sure that this private key is stored SECURELY!", address));

                context.suggestCommands(Arrays.asList(
                        BackupWalletCommand.class,
                        ImportWalletCommand.class,
                        ImportPrivateKeyCommand.class));
            }
            for (nodecore.api.grpc.Result r : reply.getResultsList())
                result.addMessage(r.getCode(), r.getMessage(), r.getDetails(), r.getError());
        } catch (StatusRuntimeException e) {
            CommandUtility.handleRuntimeException(result, e, _logger);
        }

        return result;
    }
}
