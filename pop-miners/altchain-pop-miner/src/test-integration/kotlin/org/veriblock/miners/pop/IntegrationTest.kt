package org.veriblock.miners.pop

import org.junit.Ignore
import org.junit.Rule
import org.junit.Test
import org.testcontainers.containers.BindMode
import org.testcontainers.containers.GenericContainer
import org.testcontainers.containers.output.Slf4jLogConsumer
import org.veriblock.core.utilities.createLogger

private val logger = createLogger {}

@Ignore
class IntegrationTest {
    @Rule
    @JvmField
    val apm = KGenericContainer("docker-internal.veriblock.com/altchain-pop-miner:0.4.9-rc.1.dev.9").apply {
        //networkMode = "host"
        addExposedPort(8081)
        withClasspathResourceMapping("apm.conf", "/data/application.conf", BindMode.READ_ONLY)
        withLogConsumer(Slf4jLogConsumer(logger))
        withLogConsumer {
            print(it.utf8String)
        }
    }

    @Test
    fun test() {
        println(apm.containerIpAddress)
        println(apm.firstMappedPort)
    }
}

class KGenericContainer(imageName: String) : GenericContainer<KGenericContainer>(imageName)
