// VeriBlock NodeCore CLI
// Copyright 2017-2019 Xenios SEZC
// All rights reserved.
// https://www.veriblock.org
// Distributed under the MIT software license, see the accompanying
// file LICENSE or http://www.opensource.org/licenses/mit-license.php.

package nodecore.cli;

import com.google.inject.AbstractModule;
import com.google.inject.Singleton;
import nodecore.cli.contracts.Configuration;
import nodecore.cli.contracts.ProgramOptions;
import nodecore.cli.contracts.Shell;

public class DefaultModule extends AbstractModule {
    @Override
    protected void configure() {
        bind(ProgramOptions.class)
                .to(DefaultProgramOptions.class)
                .in(Singleton.class);

        bind(Configuration.class)
                .to(DefaultConfiguration.class)
                .in(Singleton.class);

        bind(Shell.class)
                .to(DefaultShell.class)
                .in(Singleton.class);
    }
}
