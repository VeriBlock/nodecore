package nodecore.miners.pop.rules

import nodecore.miners.pop.rules.actions.MineAction
import nodecore.miners.pop.rules.actions.RuleAction
import org.koin.core.qualifier.named
import org.koin.dsl.module

@JvmField
val rulesModule = module {
    single<RuleAction<Int?>>(named("mining")) { MineAction(get()) }

    single<Set<Rule>>(override = true) {
        setOf(
            BlockHeightRule(get(named("mining")), get())
        )
    }
}
