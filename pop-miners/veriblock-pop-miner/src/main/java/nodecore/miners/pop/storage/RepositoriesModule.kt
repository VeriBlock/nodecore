package nodecore.miners.pop.storage

import com.j256.ormlite.jdbc.JdbcPooledConnectionSource
import com.j256.ormlite.support.ConnectionSource
import nodecore.miners.pop.getDatabasePath
import org.koin.dsl.module
import org.veriblock.core.utilities.Configuration
import java.sql.SQLException

@JvmField
val repositoriesModule = module {
    single<ConnectionSource> {
        val configuration: Configuration = get()
        val url = "jdbc:sqlite:${configuration.getDatabasePath()}"
        try {
            JdbcPooledConnectionSource(url)
        } catch (e: SQLException) {
            throw e
        }
    }

    single { PopRepository(get()) }
    single { KeyValueRepository(get()) }
}