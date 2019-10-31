package nodecore.miners.pop.api.controller

import io.ktor.application.call
import io.ktor.routing.Route
import io.ktor.routing.post
import nodecore.miners.pop.contracts.PoPMiner

class WalletController(
    private val miner: PoPMiner
) : ApiController {

    override fun Route.registerApi() {
        post("/api/wallet/btc/withdraw") {
            val address: String = call.parameters["address"]
                ?: throw BadRequestException("Parameter 'address' was not supplied")
            val amountString: String = call.parameters["amount"]
                ?: throw BadRequestException("Parameter 'amount' was not supplied")
            val amount = amountString.toBigDecimalOrNull()
                ?: throw BadRequestException("Parameter 'amount' is not a valid number")
            val result = miner.sendBitcoinToAddress(address, amount)
            if (result.didFail()) {
                throw CallFailureException(
                    result.messages.firstOrNull()?.let {  rm ->
                        rm.message + (rm.details.firstOrNull()?.let { "; $it" } ?: "")
                    } ?:"Failed to send $amount Bitcoins to $address"
                )
            }
        }
    }
}
