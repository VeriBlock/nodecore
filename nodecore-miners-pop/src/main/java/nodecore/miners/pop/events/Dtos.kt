package nodecore.miners.pop.events

import nodecore.miners.pop.model.VeriBlockHeader
import org.bitcoinj.core.Coin
import org.bitcoinj.core.Transaction

class CoinsReceivedEventDto(
    val tx: Transaction,
    val previousBalance: Coin,
    val newBalance: Coin
)

class NewVeriBlockFoundEventDto(
    val block: VeriBlockHeader,
    val previousHead: VeriBlockHeader?
)

class ProgramQuitEvent(
    var reason: Int
)
