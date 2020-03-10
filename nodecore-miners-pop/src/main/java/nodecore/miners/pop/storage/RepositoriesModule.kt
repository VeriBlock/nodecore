package nodecore.miners.pop.storage

import com.j256.ormlite.jdbc.JdbcPooledConnectionSource
import com.j256.ormlite.support.ConnectionSource
import nodecore.miners.pop.Configuration
import org.koin.dsl.module
import java.sql.SQLException

@JvmField
val repositoriesModule = module {
    single<ConnectionSource> {
        val configuration: Configuration = get()
        val url: String = String.format("jdbc:sqlite:%s", configuration.databasePath)
        try {
            JdbcPooledConnectionSource(url)
        } catch (e: SQLException) {
            throw e
        }
    }

    single { PopRepository(get()) }
    single { KeyValueRepository(get()) }
}
