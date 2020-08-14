package org.veriblock.altchainetl.api

import org.koin.dsl.module
import org.veriblock.core.utilities.Configuration

val apiModule = module {
    single {
        val config = get<Configuration>().extract("altchain-etl.api") ?: ApiConfig()

        ApiServer(config)
    }
}

data class ApiConfig(
    val host: String = "localhost",
    val port: Int = 8080
)
