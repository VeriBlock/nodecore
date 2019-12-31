// VeriBlock NodeCore CLI
// Copyright 2017-2019 Xenios SEZC
// All rights reserved.
// https://www.veriblock.org
// Distributed under the MIT software license, see the accompanying
// file LICENSE or http://www.opensource.org/licenses/mit-license.php.

package nodecore.cli.commands.shell;

import nodecore.cli.annotations.CommandServiceType;
import nodecore.cli.annotations.CommandSpec;
import nodecore.cli.contracts.Command;
import nodecore.cli.contracts.CommandContext;
import nodecore.cli.contracts.DefaultResult;
import nodecore.cli.contracts.Result;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@CommandSpec(
        name = "Quit",
        form = "quit",
        requiresConnection = false,
        service = CommandServiceType.SHELL,
        description = "Quit the command line shell")
public class QuitCommand implements Command {
    private static final Logger _logger = LoggerFactory.getLogger(QuitCommand.class);

    public QuitCommand() {
    }

    @Override
    public Result execute(CommandContext context) {
        context.quit();
        return new DefaultResult();
    }

}
