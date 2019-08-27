// VeriBlock NodeCore
// Copyright 2017-2019 Xenios SEZC
// All rights reserved.
// https://www.veriblock.org
// Distributed under the MIT software license, see the accompanying
// file LICENSE or http://www.opensource.org/licenses/mit-license.php.

package nodecore.cli.commands.serialization;

import nodecore.api.grpc.GetPoolStateReply;
import nodecore.api.grpc.PoolConfiguration;
import nodecore.api.grpc.PoolStats;

public class PoolStatePayload {
    public boolean running;
    public PoolConfigurationPayload configuration;
    public PoolStatsPayload stats;

    public PoolStatePayload(final GetPoolStateReply message) {
        this.running = message.getRunning();

        if (!PoolConfiguration.getDefaultInstance().equals(message.getConfiguration())) {
            this.configuration = new PoolConfigurationPayload(message.getConfiguration());
        }
        if (!PoolStats.getDefaultInstance().equals(message.getStats())) {
            this.stats = new PoolStatsPayload(message.getStats());
        }
    }
}
