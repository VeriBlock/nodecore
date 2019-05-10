// VeriBlock PoP Miner
// Copyright 2017-2019 Xenios SEZC
// All rights reserved.
// https://www.veriblock.org
// Distributed under the MIT software license, see the accompanying
// file LICENSE or http://www.opensource.org/licenses/mit-license.php.

package nodecore.miners.pop.shell.commands;

import com.google.inject.Inject;
import nodecore.miners.pop.contracts.*;
import nodecore.miners.pop.shell.annotations.CommandParameterType;
import nodecore.miners.pop.shell.annotations.CommandSpec;
import nodecore.miners.pop.shell.annotations.CommandSpecParameter;

import java.util.Arrays;
import java.util.List;

@CommandSpec(
        name = "Import Bitcoin Wallet",
        form = "importwallet",
        description = "Imports a Bitcoin wallet using comma-separated list of seed words and, optionally, a wallet creation date")
@CommandSpecParameter(name = "seedWords", required = true, type = CommandParameterType.STRING)
@CommandSpecParameter(name = "creationTime", required = false, type = CommandParameterType.LONG)
public class ImportWalletCommand implements Command {

    private final PoPMiner miner;

    @Inject
    public ImportWalletCommand(PoPMiner miner) {
        this.miner = miner;
    }

    @Override
    public Result execute(CommandContext context) throws Exception {
        Result result = new DefaultResult();

        String seedWords = context.getParameter("seedWords");
        List<String> words = Arrays.asList(seedWords.split(","));
        if (words.size() != 12) {
            result.fail();
            result.addMessage("V400", "Invalid input", "The seed words parameter should contain 12 words in a comma-separated format (no spaces)", true);
            return result;
        }

        Long creationTime = context.getParameter("creationTime");

        if (!miner.importWallet(words, creationTime)) {
            result.fail();
            result.addMessage("V500", "Unable to Import", "Unable to import the wallet from the seed supplied. Check the logs for more detail.", true);
            return result;
        }

        return result;
    }
}
