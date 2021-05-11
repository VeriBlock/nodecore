package org.veriblock.sdk.alt.plugin

import org.veriblock.sdk.alt.ExplorerBaseUrls
import org.veriblock.sdk.alt.PayoutDetectionType

data class PluginConfig(
    val pluginKey: String? = null,
    val id: Long? = null,
    val name: String? = null,
    val host: String? = null,
    val auth: HttpAuthConfig? = null,
    val payoutAddress: String? = null,
    val keystonePeriod: Int? = null,
    val neededConfirmations: Int? = null,
    val spFinalityDelay: Int? = null,
    val payoutDelay: Int? = null,
    val blockRoundIndices: List<Int>? = null,
    val autoMineRounds: List<Int> = emptyList(),
    val payoutDetectionType: PayoutDetectionType = PayoutDetectionType.COINBASE,
    val explorerBaseUrls: ExplorerBaseUrls = ExplorerBaseUrls(),
    val extraConfig: Map<String, String> = emptyMap(),
    val addressPrefix: String? = null
)

data class HttpAuthConfig(
    val username: String,
    val password: String
)
