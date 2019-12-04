package nodecore.miners.pop.rules

import nodecore.miners.pop.rules.actions.MineAction
import org.koin.core.qualifier.named
import org.koin.dsl.module

@JvmField
val rulesModule = module {
    single(named("mining")) { MineAction(get()) }

    single<Set<Rule>>(override = true) {
        setOf(
            BlockHeightRule(get(named("mining")), get())
        )
    }
}
