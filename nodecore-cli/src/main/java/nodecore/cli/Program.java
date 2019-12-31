// VeriBlock NodeCore CLI
// Copyright 2017-2019 Xenios SEZC
// All rights reserved.
// https://www.veriblock.org
// Distributed under the MIT software license, see the accompanying
// file LICENSE or http://www.opensource.org/licenses/mit-license.php.

package nodecore.cli;

import com.google.gson.GsonBuilder;
import nodecore.cli.contracts.Result;
import nodecore.cli.contracts.Shell;
import org.koin.core.KoinApplication;
import org.koin.java.KoinJavaComponent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.veriblock.core.SharedConstants;

import static nodecore.cli.CLIModuleKt.defaultModule;
import static org.koin.core.context.GlobalContext.start;

public class Program {
    private static final Logger _logger = LoggerFactory.getLogger(Program.class);

    private Program() {
    }

    private int run(String[] args) {
        System.out.print(SharedConstants.LICENSE);

        KoinApplication startupInjector = KoinApplication.create().modules(defaultModule);
        start(startupInjector);

        Object objDiagnostics = org.veriblock.core.utilities.DiagnosticUtility.getDiagnosticInfo();
        String strDiagnostics = (new GsonBuilder().setPrettyPrinting().create().toJson(objDiagnostics));
        _logger.info(strDiagnostics);

        ProgramOptions options = KoinJavaComponent.get(ProgramOptions.class);
        options.parse(args);

        Configuration configuration = KoinJavaComponent.get(Configuration.class);
        configuration.load();

        Shell shell = KoinJavaComponent.get(Shell.class);
        shell.initialize(options.getConnect());
        Result result = shell.run();
        return result.didFail() ? 1 : 0;
    }

    public static void main(String[] args) {
        Program main = new Program();
        System.exit(main.run(args));
    }
}
