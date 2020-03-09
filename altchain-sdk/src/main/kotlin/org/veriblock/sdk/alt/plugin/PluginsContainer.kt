package org.veriblock.sdk.alt.plugin

import org.veriblock.sdk.alt.SecurityInheritingChain

typealias NormalPluginsContainer = Map<String, SecurityInheritingChain>
typealias FamilyPluginsContainer = Map<String, (Long, String, String) -> SecurityInheritingChain>

class PluginsContainer(
    val normalPlugins: NormalPluginsContainer,
    val familyPlugins: FamilyPluginsContainer
)
