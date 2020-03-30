package nodecore.miners.pop.automine

import nodecore.miners.pop.AutoMineConfig

typealias Condition = (AutoMineConfig, Int) -> Boolean

val keystoneBlockCondition: Condition = { config, height ->
    config.round4 && height % 20 == 0
}

val round1Condition: Condition = { config, height ->
    config.round1 && height % 20 != 0 && height % 20 % 3 == 1
}

val round2Condition: Condition = { config, height ->
    config.round2 && height % 20 != 0 && height % 20 % 3 == 2
}

val round3Condition: Condition = { config, height ->
    config.round3 && height % 20 != 0 && height % 20 % 3 == 0
}
