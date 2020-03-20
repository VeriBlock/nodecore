// VeriBlock PoW CPU Miner
// Copyright 2017-2020 Xenios SEZC
// All rights reserved.
// https://www.veriblock.org
// Distributed under the MIT software license, see the accompanying
// file LICENSE or http://www.opensource.org/licenses/mit-license.php.

package nodecore.miners.pow;

import java.util.ArrayList;

class ShareRepo {
    private int validShares = 0;
    private int invalidShares = 0;

    public ShareRepo(int startingValid, int startingInvalid) {
        this.validShares = startingValid;
        this.invalidShares = startingInvalid;
    }

    private ArrayList<FoundSharePackage> pendingShares = new ArrayList<>();

    void addShare(FoundSharePackage sharePackage) {
        this.pendingShares.add(sharePackage);
    }

    ArrayList<FoundSharePackage> getAllShares() {
        ArrayList<FoundSharePackage> toReturn = new ArrayList<>(pendingShares);
        pendingShares = new ArrayList<>();
        return toReturn;
    }

    boolean hasShares() {
        return pendingShares.size() > 0;
    }

    void countValidShare() {
        validShares++;
    }

    void countInvalidShare() {
        invalidShares++;
    }

    int getValidShares() {
        return validShares;
    }

    int getInvalidShares() {
        return invalidShares;
    }
}
