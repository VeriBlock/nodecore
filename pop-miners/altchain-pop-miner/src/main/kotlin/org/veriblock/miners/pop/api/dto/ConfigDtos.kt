package org.veriblock.miners.pop.api.dto

import com.papsign.ktor.openapigen.annotations.Request
import com.papsign.ktor.openapigen.annotations.Response

@Request("Auto mine configuration request")
@Response("Auto mine configuration")
data class AutoMineConfigDto(
    val automineRounds: List<AutoMineRound>
)

data class AutoMineRound(
    val round: Int,
    val enabled: Boolean
)

@Request("Vbk fee configuration request")
@Response("Vbk fee configuration")
class VbkFeeConfigDto(
    val maxFee: Long?,
    val feePerByte: Long?
)
