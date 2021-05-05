package nodecore.p2p

import io.mockk.every
import io.mockk.mockk
import org.veriblock.core.params.MainNetParameters
import org.veriblock.core.params.NetworkParameters
import java.math.BigInteger

fun createFakeParameters(
    network: String = MainNetParameters.NETWORK,
    minDifficulty: BigInteger = BigInteger.ONE,
    proVersion: Int = 0
): NetworkParameters = mockk(
    relaxed = true
) {
    every { name } returns network
    every { rpcPort } returns 10500
    every { p2pPort } returns 7500

    every { bootstrapDns } returns "seed.veriblock.org"
    every { protocolVersion } returns proVersion

    every { minimumDifficulty } returns minDifficulty
    every { progPowForkHeight } returns Int.MAX_VALUE
}
