// VeriBlock NodeCore CLI
// Copyright 2017-2019 Xenios SEZC
// All rights reserved.
// https://www.veriblock.org
// Distributed under the MIT software license, see the accompanying
// file LICENSE or http://www.opensource.org/licenses/mit-license.php.

package nodecore.cli.commands.rpc;

import com.google.inject.Inject;
import io.grpc.StatusRuntimeException;
import nodecore.api.grpc.ImportPrivateKeyReply;
import nodecore.api.grpc.ImportPrivateKeyRequest;
import nodecore.api.grpc.utilities.ByteStringUtility;
import nodecore.cli.annotations.CommandParameterType;
import nodecore.cli.annotations.CommandSpec;
import nodecore.cli.annotations.CommandSpecParameter;
import nodecore.cli.commands.serialization.FormattableObject;
import nodecore.cli.commands.serialization.ImportPrivateKeyInfo;
import nodecore.cli.contracts.Command;
import nodecore.cli.contracts.CommandContext;
import nodecore.cli.contracts.DefaultResult;
import nodecore.cli.contracts.Result;
import nodecore.cli.utilities.CommandUtility;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Arrays;

@CommandSpec(
        name = "Import Private Key",
        form = "importprivatekey",
        description = "Imports the provided private key into NodeCore")

@CommandSpecParameter(name = "privateKey", required = true, type = CommandParameterType.HEXSTRING)
public class ImportPrivateKeyCommand implements Command {
    private static final Logger _logger = LoggerFactory.getLogger(ImportPrivateKeyCommand.class);

    @Inject
    public ImportPrivateKeyCommand() {
    }

    @Override
    public Result execute(CommandContext context) {
        Result result = new DefaultResult();

        try {
            String privateKeyHex = context.getParameter("privateKey");
            ImportPrivateKeyReply reply;

            reply = context
                    .adminService()
                    .importPrivateKey(ImportPrivateKeyRequest.newBuilder()
                            .setPrivateKey(ByteStringUtility.hexToByteString(privateKeyHex))
                            .build());

            if (!reply.getSuccess()) {
                result.fail();
            } else {
                FormattableObject<ImportPrivateKeyInfo> temp = new FormattableObject<>(reply.getResultsList());
                temp.success = !result.didFail();
                temp.payload = new ImportPrivateKeyInfo(reply);

                context.outputObject(temp);

                context.suggestCommands(Arrays.asList(
                        DumpPrivateKeyCommand.class,
                        BackupWalletCommand.class,
                        ImportWalletCommand.class
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
