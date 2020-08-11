package org.veriblock.altchainetl.api

import io.ktor.application.*
import io.ktor.features.*
import io.ktor.locations.*
import io.ktor.metrics.micrometer.*
import io.ktor.response.*
import io.ktor.routing.*
import io.ktor.serialization.*
import io.ktor.server.engine.*
import io.ktor.server.netty.*
import io.micrometer.core.instrument.binder.jvm.ClassLoaderMetrics
import io.micrometer.core.instrument.binder.jvm.JvmGcMetrics
import io.micrometer.core.instrument.binder.jvm.JvmMemoryMetrics
import io.micrometer.core.instrument.binder.jvm.JvmThreadMetrics
import io.micrometer.core.instrument.binder.logging.LogbackMetrics
import io.micrometer.core.instrument.binder.system.ProcessorMetrics
import io.micrometer.core.instrument.binder.system.UptimeMetrics
import io.micrometer.prometheus.PrometheusConfig
import io.micrometer.prometheus.PrometheusMeterRegistry
import org.veriblock.altchainetl.api.feature.statusPages

const val API_VERSION = "0.1.0"

class ApiServer(private val config: ApiConfig) {
    fun start() = embeddedServer(
        factory = Netty,
        port = config.port,
        host = config.host
    ) {
        install(CORS) {
            anyHost()
            allowNonSimpleContentTypes = true
        }
        install(DefaultHeaders)
        install(CallLogging)
        install(ContentNegotiation) {
            json()
        }
        install(Locations)
        statusPages()
        val metricsRegistry = PrometheusMeterRegistry(PrometheusConfig.DEFAULT)
        install(MicrometerMetrics) {
            registry = metricsRegistry
            meterBinders = listOf(
                UptimeMetrics(),
                ProcessorMetrics(),
                LogbackMetrics(),
                JvmThreadMetrics(),
                JvmMemoryMetrics(),
                JvmGcMetrics(),
                ClassLoaderMetrics()
            )
        }

        routing {
            get("/") {
                call.respondRedirect("api")
            }
            route("api") {
                get("/") {
                    call.respond("Altchain ETL API v$API_VERSION")
                }
                get("metrics") {
                    call.respond(metricsRegistry.scrape())
                }
            }
        }
    }.start()
}
