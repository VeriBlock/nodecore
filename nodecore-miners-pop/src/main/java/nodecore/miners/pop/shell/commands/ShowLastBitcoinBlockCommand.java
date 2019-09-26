// VeriBlock PoP Miner
// Copyright 2017-2019 Xenios SEZC
// All rights reserved.
// https://www.veriblock.org
// Distributed under the MIT software license, see the accompanying
// file LICENSE or http://www.opensource.org/licenses/mit-license.php.

package nodecore.miners.pop.shell.commands;

import com.google.inject.Inject;
import nodecore.miners.pop.common.Utility;
import nodecore.miners.pop.contracts.*;
import nodecore.miners.pop.shell.annotations.CommandSpec;
import org.bitcoinj.core.Block;
import org.bitcoinj.core.Coin;
import org.bitcoinj.core.StoredBlock;
import org.bitcoinj.core.Utils;

import java.io.ByteArrayOutputStream;

@CommandSpec(
        name = "Show Last Bitcoin Block",
        form = "showlastbitcoinblock",
        description = "Displays information about the most recent Bitcoin block")
public class ShowLastBitcoinBlockCommand implements Command {
    private final PoPMiner popMiner;

    @Inject
    public ShowLastBitcoinBlockCommand(PoPMiner popMiner) {
        this.popMiner = popMiner;
    }

    @Override
    public Result execute(CommandContext context) throws Exception {
        Result result = new DefaultResult();

        StoredBlock lastBlock = popMiner.getLastBitcoinBlock();
        Block lastBlockHeader = lastBlock.getHeader();

        ByteArrayOutputStream headerOutputSteram = new ByteArrayOutputStream();

        Utils.uint32ToByteStreamLE(lastBlockHeader.getVersion(), headerOutputSteram);
        headerOutputSteram.write(lastBlockHeader.getPrevBlockHash().getReversedBytes());
        headerOutputSteram.write(lastBlockHeader.getMerkleRoot().getReversedBytes());
        Utils.uint32ToByteStreamLE(lastBlockHeader.getTimeSeconds(), headerOutputSteram);
        Utils.uint32ToByteStreamLE(lastBlockHeader.getDifficultyTarget(), headerOutputSteram);
        Utils.uint32ToByteStreamLE(lastBlockHeader.getNonce(), headerOutputSteram);

        result.addMessage("V200", "Success", Utility.bytesToHex(headerOutputSteram.toByteArray()), false);

        context.writeToOutput("Bitcoin Block Header: %s", Utility.bytesToHex(headerOutputSteram.toByteArray()));
        context.writeToOutput("Bitcoin Block Hash: %s", lastBlockHeader.getHash());
        context.writeToOutput("Bitcoin Block Height: %s", lastBlock.getHeight());
        context.flush();

        return result;
    }
}