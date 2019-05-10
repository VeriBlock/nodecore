// VeriBlock NodeCore
// Copyright 2017-2019 Xenios SEZC
// All rights reserved.
// https://www.veriblock.org
// Distributed under the MIT software license, see the accompanying
// file LICENSE or http://www.opensource.org/licenses/mit-license.php.

package nodecore.miners.pop.storage;

import com.google.inject.*;
import com.j256.ormlite.jdbc.JdbcPooledConnectionSource;
import com.j256.ormlite.support.ConnectionSource;
import nodecore.miners.pop.contracts.Configuration;
import nodecore.miners.pop.contracts.KeyValueRepository;
import nodecore.miners.pop.contracts.PoPRepository;

public class RepositoriesModule extends AbstractModule {
    @Override
    protected void configure() {
        bind(ConnectionSource.class)
                .toProvider(ConnectionSourceProvider.class)
                .in(Scopes.SINGLETON);

        bind(PoPRepository.class)
                .to(OrmLitePoPRepository.class)
                .in(Singleton.class);

        bind(KeyValueRepository.class)
                .to(OrmLiteKeyValueRepository.class)
                .in(Singleton.class);
    }

    public static class ConnectionSourceProvider implements Provider<ConnectionSource> {
        private final String url;

        @Inject
        public ConnectionSourceProvider(Configuration configuration) {
            url = String.format("jdbc:sqlite:%s", configuration.getDatabasePath());
        }

        @Override
        public ConnectionSource get() {
            try {
                return new JdbcPooledConnectionSource(url);
            } catch (Exception ignored) {
                return null;
            }
        }
    }
}
