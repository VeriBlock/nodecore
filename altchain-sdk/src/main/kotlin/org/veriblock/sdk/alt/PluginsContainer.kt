package org.veriblock.sdk.alt

typealias NormalPluginsContainer = Map<String, SecurityInheritingChain>
typealias FamilyPluginsContainer = Map<String, (Long, String, String) -> SecurityInheritingChain>

class PluginsContainer(
    val normalPlugins: NormalPluginsContainer,
    val familyPlugins: FamilyPluginsContainer
)
