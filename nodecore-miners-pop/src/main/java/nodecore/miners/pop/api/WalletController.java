package nodecore.miners.pop.api;

import com.google.inject.Inject;
import nodecore.miners.pop.api.annotations.Route;
import nodecore.miners.pop.contracts.PoPMiner;
import nodecore.miners.pop.contracts.Result;
import nodecore.miners.pop.contracts.ResultMessage;
import org.apache.commons.lang3.StringUtils;
import spark.Request;
import spark.Response;

import java.math.BigDecimal;

import static nodecore.miners.pop.api.annotations.Route.Verb.POST;

public class WalletController extends ApiController {
    private final PoPMiner miner;

    @Inject
    public WalletController(PoPMiner miner) {
        this.miner = miner;
    }

    @Route(path = "/api/wallet/btc/withdraw", verb = POST)
    public String withdrawBitcoinToAddress(Request request, Response response) {

        String address = request.queryParams("address");
        if (address == null) {
            response.status(400);
            return "Parameter 'address' was not supplied";
        }

        String amountString = request.queryParams("amount");
        if (amountString == null) {
            response.status(400);
            return "Parameter 'amount' was not supplied";
        }
        if (!StringUtils.isNumeric(amountString)) {
            response.status(400);
            return "Parameter 'amount' is not a valid number";
        }

        BigDecimal amount = BigDecimal.valueOf(Long.parseLong(amountString));
        try {
            Result result = miner.sendBitcoinToAddress(address, amount);
            if (result.didFail()) {
                response.status(500);

                if (!result.getMessages().isEmpty()) {
                    ResultMessage rm = result.getMessages().get(0);
                    String responseMessage = rm.getMessage();
                    if (!rm.getDetails().isEmpty()) {
                        responseMessage += "; " + rm.getDetails().get(0);
                    }
                    return responseMessage;
                }
                return "Failed to send " + amount + " Bitcoins to " + address;
            }
        } catch (Exception e) {
            response.status(500);
            return e.getMessage();
        }
        response.status(200);

        return "";
    }
}
