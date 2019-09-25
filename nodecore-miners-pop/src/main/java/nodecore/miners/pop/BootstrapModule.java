// VeriBlock PoP Miner
// Copyright 2017-2019 Xenios SEZC
// All rights reserved.
// https://www.veriblock.org
// Distributed under the MIT software license, see the accompanying
// file LICENSE or http://www.opensource.org/licenses/mit-license.php.

package nodecore.miners.pop;

import com.google.inject.AbstractModule;
import com.google.inject.Singleton;
import nodecore.miners.pop.contracts.*;
import nodecore.miners.pop.services.*;
import org.bitcoinj.core.Context;

public class BootstrapModule extends AbstractModule {
    @Override
    protected void configure() {
        bind(ProgramOptions.class)
                .to(DefaultProgramOptions.class)
                .in(Singleton.class);

        bind(Configuration.class)
                .to(DefaultConfiguration.class)
                .in(Singleton.class);

        bind(Context.class)
                .toProvider(ContextProvider.class)
                .in(Singleton.class);

        bind(MessageService.class)
                .to(DefaultMessageService.class)
                .in(Singleton.class);

        bind(PoPMiner.class)
                .to(DefaultPoPMiner.class)
                .in(Singleton.class);

        bind (PoPStateService.class)
                .to(DefaultPoPStateService.class)
                .in(Singleton.class);

        bind(NodeCoreService.class)
                .to(DefaultNodeCoreService.class)
                .in(Singleton.class);

        bind(BitcoinService.class)
                .to(DefaultBitcoinService.class)
                .in(Singleton.class);

        bind(PoPMiningScheduler.class)
                .to(DefaultPoPMiningScheduler.class)
                .in(Singleton.class);
        
        bind(RebootScheduler.class)
        	.to(DefaultRebootScheduler.class)
        	.in(Singleton.class);
    }
}
