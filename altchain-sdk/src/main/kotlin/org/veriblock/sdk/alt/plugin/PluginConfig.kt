package org.veriblock.sdk.alt.plugin

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
    val payoutInterval: Int? = null,
    val blockRoundIndices: List<Int>? = null,
    val autoMineRounds: List<Int> = emptyList(),
    val extraConfig: Map<String, String> = emptyMap()
)

data class HttpAuthConfig(
    val username: String,
    val password: String
)
