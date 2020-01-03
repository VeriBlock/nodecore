// VeriBlock NodeCore
// Copyright 2017-2019 Xenios SEZC
// All rights reserved.
// https://www.veriblock.org
// Distributed under the MIT software license, see the accompanying
// file LICENSE or http://www.opensource.org/licenses/mit-license.php.

package nodecore.cli.commands;

import com.google.gson.GsonBuilder;
import nodecore.cli.serialization.FormattableObject;
import nodecore.cli.contracts.CommandContext;
import nodecore.cli.contracts.OutputWriter;
import nodecore.cli.contracts.Shell;
import org.jline.utils.AttributedStyle;

public class ShellWriter implements OutputWriter {
    private final Shell shell;

    public ShellWriter(Shell shell) {
        this.shell = shell;
    }

    public void outputObject(CommandContext context, FormattableObject obj) {
        format(AttributedStyle.BOLD, AttributedStyle.GREEN, "%s\n\n", new GsonBuilder().setPrettyPrinting().create().toJson(obj.payload));
    }

    public void format(
            AttributedStyle style,
            int foregroundColor,
            String fmt, Object... args) {
        shell.format(style, foregroundColor, String.format( fmt, args));
    }

    public void warning(String message) {
        format(AttributedStyle.BOLD, AttributedStyle.YELLOW, "%s\n\n", message);
    }

    public void info(String message) {
        format(AttributedStyle.BOLD, AttributedStyle.WHITE, "%s\n\n", message);
    }

    public void status(String message) {

        //Ensure line separator is added so that flush command will work
        message = message + System.getProperty("line.separator");

        //this override will flush to output:
        shell.format(message, new Object[0]);

    }
    public void normal(String message) {
        format(AttributedStyle.DEFAULT, AttributedStyle.WHITE, message);
    }

    public void bold(String message) {
        format(AttributedStyle.BOLD, AttributedStyle.CYAN, message);
    }

    public void inverted(String message) {
        format(AttributedStyle.INVERSE, AttributedStyle.WHITE, message);
    }
}
