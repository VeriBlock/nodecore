// VeriBlock Blockchain Project
// Copyright 2017-2018 VeriBlock, Inc
// Copyright 2018-2019 Xenios SEZC
// All rights reserved.
// https://www.veriblock.org
// Distributed under the MIT software license, see the accompanying
// file LICENSE or http://www.opensource.org/licenses/mit-license.php.

package org.veriblock.miners.pop.storage

import com.j256.ormlite.jdbc.JdbcPooledConnectionSource
import com.j256.ormlite.support.ConnectionSource
import org.koin.dsl.module.module
import org.veriblock.lite.core.Context
import org.veriblock.miners.pop.storage.ormlite.OrmLiteKeyValueRepository
import org.veriblock.miners.pop.storage.ormlite.OrmLitePoPRepository
import java.sql.SQLException

val sqliteDbFile = Context.directory.resolve("pop.dat")

val repositoryModule = module {
    single<ConnectionSource> {
        val url = "jdbc:sqlite:$sqliteDbFile"
        try {
            JdbcPooledConnectionSource(url)
        } catch (e: SQLException) {
            throw e
        }
    }

    single <OperationRepository> { OrmLitePoPRepository(get()) }
    single <KeyValueRepository> { OrmLiteKeyValueRepository(get()) }
}
