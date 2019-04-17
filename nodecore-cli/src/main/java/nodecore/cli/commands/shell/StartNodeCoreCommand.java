// VeriBlock NodeCore CLI
// Copyright 2017-2019 Xenios SEZC
// All rights reserved.
// https://www.veriblock.org
// Distributed under the MIT software license, see the accompanying
// file LICENSE or http://www.opensource.org/licenses/mit-license.php.

package nodecore.cli.commands.shell;

import com.google.inject.Inject;
import nodecore.cli.annotations.CommandServiceType;
import nodecore.cli.annotations.CommandSpec;
import nodecore.cli.commands.serialization.EmptyPayload;
import nodecore.cli.commands.serialization.FormattableObject;
import nodecore.cli.contracts.Command;
import nodecore.cli.contracts.CommandContext;
import nodecore.cli.contracts.DefaultResult;
import nodecore.cli.contracts.Result;
import nodecore.cli.utilities.ExternalProgramUtilities;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.awt.*;
import java.util.Collections;

@CommandSpec(
        name = "Start NodeCore",
        form = "startnodecore",
        requiresConnection = false,
        service = CommandServiceType.SHELL,
        description = "Attempts to start NodeCore in a new window")
public class StartNodeCoreCommand implements Command {
    private static final Logger _logger = LoggerFactory.getLogger(StartNodeCoreCommand.class);

    @Inject
    public StartNodeCoreCommand() {
    }

    @Override
    public Result execute(CommandContext context) {
        Result result = new DefaultResult();

        if (GraphicsEnvironment.isHeadless()) {
            result.addMessage("V004",
                    "startnodecore command unavailable!",
                    "The startnodecore command is not available in headless mode!",
                    true);
            result.fail();
            return result;
        }
        String successFile = ExternalProgramUtilities.startupExternalProcess(result, "../../", "nodecore-0", "nodecore", "NodeCore");

        if (successFile != null) {
            FormattableObject<EmptyPayload> temp = new FormattableObject<>(Collections.emptyList());
            temp.messages.add(new FormattableObject.MessageInfo("V200", "Started", "Successfully started NodeCore " + successFile, false));
            temp.success = !result.didFail();
            temp.payload = new EmptyPayload();

            context.outputObject(temp);

            context.write().bold("Please note that NodeCore may take several minutes to load before you can connect to it!");
        }

        return result;
    }
}
