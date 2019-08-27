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
import nodecore.api.grpc.BackupWalletReply;
import nodecore.api.grpc.BackupWalletRequest;
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
        name = "Backup Wallet",
        form = "backupwallet",
        description = "Backup the wallet of a NodeCore instance")
@CommandSpecParameter(name = "targetLocation", required = true, type = CommandParameterType.STRING)
public class BackupWalletCommand implements Command {
    private static final Logger _logger = LoggerFactory.getLogger(SendCommand.class);

    @Inject
    public BackupWalletCommand(){
    }

    @Override
    public Result execute(CommandContext context) {
        Result result = new DefaultResult();
        String targetLocation = context.getParameter("targetLocation");

        try {
            BackupWalletReply reply = context
                    .adminService()
                    .backupWallet(BackupWalletRequest
                            .newBuilder()
                            .setTargetLocation(ByteString.copyFrom(targetLocation.getBytes()))
                            .build());
            if (!reply.getSuccess()) {
                result.fail();
            } else {
                FormattableObject<EmptyPayload> temp = new FormattableObject<>(reply.getResultsList());
                temp.success = !result.didFail();
                temp.payload = new EmptyPayload();

                context.outputObject(temp);

                context.write().warning("Note: The backed-up wallet file is saved on the computer where NodeCore is running.");
                context.write().warning("Note: If the wallet is encrypted, the backup will require the password in use at the time the backup was created.");

                context.suggestCommands(Arrays.asList(
                        ImportWalletCommand.class,
                        DumpPrivateKeyCommand.class,
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
