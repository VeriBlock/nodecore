package org.veriblock.altchainetl

import mu.KLogger
import org.koin.core.context.startKoin
import org.koin.dsl.module
import org.veriblock.altchainetl.api.ApiServer
import org.veriblock.altchainetl.api.apiModule
import org.veriblock.altchainetl.persistence.persistenceModule
import org.veriblock.altchainetl.service.serviceModule
import org.veriblock.core.utilities.Configuration
import org.veriblock.core.utilities.createLogger

val logger: KLogger = createLogger {}

fun main() {
    val koin = startKoin {
        modules(listOf(
            module {
                single { Configuration() }
            },
            apiModule,
            serviceModule,
            persistenceModule
        ))
    }.koin

    val apiServer = koin.get<ApiServer>()
    apiServer.start()
}
