// VeriBlock NodeCore
// Copyright 2017-2019 Xenios SEZC
// All rights reserved.
// https://www.veriblock.org
// Distributed under the MIT software license, see the accompanying
// file LICENSE or http://www.opensource.org/licenses/mit-license.php.

package nodecore.miners.pop.api;

import com.google.inject.AbstractModule;
import com.google.inject.multibindings.Multibinder;

public class WebApiModule extends AbstractModule {
    @Override
    protected void configure() {
        Multibinder<ApiController> controllers = Multibinder.newSetBinder(binder(), ApiController.class);
        controllers.addBinding().to(MiningController.class);
        controllers.addBinding().to(ConfigurationController.class);
        controllers.addBinding().to(QuitController.class);
        controllers.addBinding().to(LastBitcoinBlockController.class);
    }
}