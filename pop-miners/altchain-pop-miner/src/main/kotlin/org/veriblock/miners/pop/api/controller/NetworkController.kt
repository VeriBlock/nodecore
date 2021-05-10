package org.veriblock.miners.pop.api.controller

import com.papsign.ktor.openapigen.annotations.Path
import com.papsign.ktor.openapigen.annotations.parameters.QueryParam
import com.papsign.ktor.openapigen.route.info
import com.papsign.ktor.openapigen.route.path.normal.NormalOpenAPIRoute
import com.papsign.ktor.openapigen.route.path.normal.get
import com.papsign.ktor.openapigen.route.response.respond
import com.papsign.ktor.openapigen.route.route
import org.veriblock.miners.pop.MinerConfig
import org.veriblock.miners.pop.api.dto.ExplorerBaseUrlsResponse
import org.veriblock.miners.pop.api.dto.NetworkInfoResponse
import org.veriblock.miners.pop.api.dto.toExplorerBaseUrlsResponse
import org.veriblock.sdk.alt.plugin.PluginService

class NetworkController(
    val minerConfig: MinerConfig,
    private val pluginService: PluginService
) : ApiController {

    @Path("explorer-urls")
    class ExplorerUrlsPath(
        @QueryParam("The chain key") val chain: String
    )

    override fun NormalOpenAPIRoute.registerApi() = route("network") {
        get<Unit, NetworkInfoResponse>(
            info("Returns the Blockchain Network this APM is configured with")
        ) {
            val networkType = minerConfig.network
            val vbkBaseUrl = if (networkType.equals("mainnet", true)) {
                "https://explore.veriblock.org"
            } else {
                "https://testnet.explore.veriblock.org"
            }
            val explorerBaseUrlsResponse = ExplorerBaseUrlsResponse(
                blockByHeight = "$vbkBaseUrl/block/",
                blockByHash ="$vbkBaseUrl/block/",
                transactionById = "$vbkBaseUrl/tx/",
                atvById = "$vbkBaseUrl/tx/"
            )
            respond(NetworkInfoResponse(networkType, explorerBaseUrlsResponse))
        }
        get<ExplorerUrlsPath, ExplorerBaseUrlsResponse>(
            info("Returns the explorer base urls for the the altchain")
        ) { params ->
            val plugin = pluginService[params.chain]
                ?: throw NotFoundException("There is no SI chain with the key ${params.chain}")
            respond(plugin.config.explorerBaseUrls.toExplorerBaseUrlsResponse())
        }
    }
}
