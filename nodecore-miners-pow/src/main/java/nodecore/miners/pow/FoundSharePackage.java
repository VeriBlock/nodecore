// VeriBlock PoW CPU Miner
// Copyright 2017-2020 Xenios SEZC
// All rights reserved.
// https://www.veriblock.org
// Distributed under the MIT software license, see the accompanying
// file LICENSE or http://www.opensource.org/licenses/mit-license.php.


package nodecore.miners.pow;

class FoundSharePackage {

    private final int jobId;
    private final int timestamp;
    private final int nonce;
    private final long extraNonce;
    private final String hash;
    private final String previousHash;

    FoundSharePackage(int jobId, int timestamp, int nonce, long extraNonce, String hash, String previousHash) {
        this.jobId = jobId;
        this.timestamp = timestamp;
        this.nonce = nonce;
        this.extraNonce = extraNonce;
        this.hash = hash;
        this.previousHash = previousHash;
    }

    int getJobId() {
        return jobId;
    }

    int getTimestamp() {
        return timestamp;
    }

    int getNonce() {
        return nonce;
    }

    long getExtraNonce() { return extraNonce; }

    String getHash() {
        return hash;
    }

    String getPreviousHash() {
        return previousHash;
    }
}
