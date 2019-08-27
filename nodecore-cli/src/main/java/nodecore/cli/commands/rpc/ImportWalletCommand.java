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
import nodecore.api.grpc.ImportWalletReply;
import nodecore.api.grpc.ImportWalletRequest;
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
        name = "Import Wallet",
        form = "importwallet",
        description = "Import a NodeCore wallet backup")
@CommandSpecParameter(name = "sourceLocation", required = true, type = CommandParameterType.STRING)
public class ImportWalletCommand implements Command {
    private static final Logger _logger = LoggerFactory.getLogger(SendCommand.class);

    @Inject
    public ImportWalletCommand() {}

    @Override
    public Result execute(CommandContext context) {
        Result result = new DefaultResult();
        String sourceLocation = context.getParameter("sourceLocation");

        try {
            String passphrase = context.passwordPrompt("Enter passphrase of importing wallet (Press ENTER if not password-protected): ");

            ImportWalletRequest.Builder requestBuilder = ImportWalletRequest.newBuilder();
            requestBuilder.setSourceLocation(ByteString.copyFrom(sourceLocation.getBytes()));
            if (passphrase != null && passphrase.length() > 0) {
                requestBuilder.setPassphrase(passphrase);
            }

            ImportWalletReply reply = context
                    .adminService()
                    .importWallet(requestBuilder.build());
            if (!reply.getSuccess()) {
                result.fail();
            } else {
                FormattableObject<EmptyPayload> temp = new FormattableObject<>(reply.getResultsList());
                temp.success = !result.didFail();
                temp.payload = new EmptyPayload();

                context.outputObject(temp);

                context.suggestCommands(Arrays.asList(
                        DumpPrivateKeyCommand.class,
                        ImportPrivateKeyCommand.class,
                        BackupWalletCommand.class
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
