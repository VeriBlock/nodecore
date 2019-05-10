// VeriBlock PoP Miner
// Copyright 2017-2019 Xenios SEZC
// All rights reserved.
// https://www.veriblock.org
// Distributed under the MIT software license, see the accompanying
// file LICENSE or http://www.opensource.org/licenses/mit-license.php.

package nodecore.miners.pop;

import com.google.inject.Inject;
import com.google.inject.Provider;
import nodecore.miners.pop.contracts.Configuration;
import org.bitcoinj.core.Context;
import org.bitcoinj.core.NetworkParameters;
import org.bitcoinj.params.MainNetParams;
import org.bitcoinj.params.RegTestParams;
import org.bitcoinj.params.TestNet3Params;

public class ContextProvider implements Provider<Context> {
    private final Configuration configuration;

    @Inject
    public ContextProvider(Configuration configuration) {
        this.configuration = configuration;
    }

    @Override
    public Context get() {
        NetworkParameters params;
        switch (configuration.getBitcoinNetwork()) {
            case MainNet:
                params = MainNetParams.get();
                break;
            case TestNet:
                params = TestNet3Params.get();
                break;
            case RegTest:
                params = RegTestParams.get();
                break;
            default:
                params = RegTestParams.get();
                break;
        }

        return new Context(params);
    }
}
