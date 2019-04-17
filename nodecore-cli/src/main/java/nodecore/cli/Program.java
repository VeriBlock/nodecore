// VeriBlock NodeCore CLI
// Copyright 2017-2019 Xenios SEZC
// All rights reserved.
// https://www.veriblock.org
// Distributed under the MIT software license, see the accompanying
// file LICENSE or http://www.opensource.org/licenses/mit-license.php.

package nodecore.cli;

import com.google.gson.GsonBuilder;
import com.google.inject.Guice;
import com.google.inject.Injector;
import nodecore.cli.commands.CommandFactoryModule;
import nodecore.cli.contracts.Configuration;
import nodecore.cli.contracts.ProgramOptions;
import nodecore.cli.contracts.Result;
import nodecore.cli.contracts.Shell;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.veriblock.core.SharedConstants;

public class Program {
    private static final Logger _logger = LoggerFactory.getLogger(Program.class);

    private Program() {
    }

    private int run(String[] args) {
        System.out.print(SharedConstants.LICENSE);

        Injector injector = Guice.createInjector(
                new DefaultModule(),
                new CommandFactoryModule());

        Object objDiagnostics = org.veriblock.core.utilities.DiagnosticUtility.getDiagnosticInfo();
        String strDiagnostics = (new GsonBuilder().setPrettyPrinting().create().toJson(objDiagnostics));
        _logger.info(strDiagnostics);

        ProgramOptions options = injector.getInstance(ProgramOptions.class);
        options.parse(args);

        Configuration configuration = injector.getInstance(Configuration.class);
        configuration.load();

        Shell shell = injector.getInstance(Shell.class);
        shell.initialize(options.getConnect());
        Result result = shell.run();
        return result.didFail() ? 1 : 0;
    }

    public static void main(String[] args) {
        Program main = new Program();
        System.exit(main.run(args));
    }
}
