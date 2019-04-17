// VeriBlock NodeCore CLI
// Copyright 2017-2019 Xenios SEZC
// All rights reserved.
// https://www.veriblock.org
// Distributed under the MIT software license, see the accompanying
// file LICENSE or http://www.opensource.org/licenses/mit-license.php.

package nodecore.cli.commands;

import com.google.inject.AbstractModule;
import com.google.inject.Singleton;
import com.google.inject.multibindings.MapBinder;
import nodecore.cli.contracts.Command;
import nodecore.cli.contracts.CommandFactory;

public class CommandFactoryModule extends AbstractModule {

    @Override
    @SuppressWarnings("unchecked")
    protected void configure() {
        DefaultCommandFactory.buildFactoryCache();

        MapBinder<String, Command> binder = MapBinder.newMapBinder(binder(), String.class, Command.class);
        for (String key : DefaultCommandFactory._definitions.keySet())
            binder.addBinding(key).to(DefaultCommandFactory._definitions.get(key).getCommandClass());

        bind(CommandFactory.class)
                .to(DefaultCommandFactory.class)
                .in(Singleton.class);
    }
}
