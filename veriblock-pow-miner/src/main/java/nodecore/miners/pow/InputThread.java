// VeriBlock PoW CPU Miner
// Copyright 2017-2020 Xenios SEZC
// All rights reserved.
// https://www.veriblock.org
// Distributed under the MIT software license, see the accompanying
// file LICENSE or http://www.opensource.org/licenses/mit-license.php.

package nodecore.miners.pow;

import nodecore.api.ucp.commands.UCPClientCommand;
import nodecore.api.ucp.commands.UCPIncomingCommandParser;
import nodecore.api.ucp.commands.client.MiningJob;
import nodecore.api.ucp.commands.client.MiningMempoolUpdate;
import nodecore.api.ucp.commands.client.MiningSubmitFailure;
import nodecore.api.ucp.commands.client.MiningSubmitSuccess;

import java.io.BufferedReader;

public class InputThread extends Thread {
    private final BufferedReader in;
    private final ShareRepo shareRepo;
    private MinerThreadManager minerThreadManager;
    private boolean isRunning = true;

    InputThread(BufferedReader in, MinerThreadManager minerThreadManager, ShareRepo shareRepo) {
        this.in = in;
        this.minerThreadManager = minerThreadManager;
        this.shareRepo = shareRepo;
    }

    public void run() {
        while (true) {
            try {
                String update = in.readLine();
                UCPClientCommand updateCommand = UCPIncomingCommandParser.parseClientCommand(update);
                System.out.println("Update: " + update);

                if (updateCommand instanceof MiningJob) {
                    System.out.println("New job received!");
                    minerThreadManager.update(updateCommand);
                } else if (updateCommand instanceof MiningMempoolUpdate) {
                    System.out.println("New Mempool Update Received for Current Round!");
                    minerThreadManager.update(updateCommand);
                } else if (updateCommand instanceof MiningSubmitSuccess) {
                    System.out.println("Share successfully submitted!");
                    shareRepo.countValidShare();
                } else if (updateCommand instanceof MiningSubmitFailure) {
                    System.out.println("Share rejected! Reason: " + ((MiningSubmitFailure)updateCommand).getReason());
                    shareRepo.countInvalidShare();
                }
                else {
                    System.out.println("An unexpected command was received: " + update + "!");
                }

            } catch (Exception e) {
                System.out.println("Failed to read command from server...");
                isRunning = false;
                break;
            }
        }

        minerThreadManager.shutdown();
    }

    public boolean isRunning() {
        return isRunning;
    }
}
