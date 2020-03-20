// VeriBlock NodeCore
// Copyright 2017-2020 Xenios SEZC
// All rights reserved.
// https://www.veriblock.org
// Distributed under the MIT software license, see the accompanying
// file LICENSE or http://www.opensource.org/licenses/mit-license.php.

package nodecore.cli.serialization;

import nodecore.api.grpc.VeriBlockMessages;

public class PoolStatePayload {
    public boolean running;
    public PoolConfigurationPayload configuration;
    public PoolStatsPayload stats;

    public PoolStatePayload(final VeriBlockMessages.GetPoolStateReply message) {
        this.running = message.getRunning();

        if (!VeriBlockMessages.PoolConfiguration.getDefaultInstance().equals(message.getConfiguration())) {
            this.configuration = new PoolConfigurationPayload(message.getConfiguration());
        }
        if (!VeriBlockMessages.PoolStats.getDefaultInstance().equals(message.getStats())) {
            this.stats = new PoolStatsPayload(message.getStats());
        }
    }
}
