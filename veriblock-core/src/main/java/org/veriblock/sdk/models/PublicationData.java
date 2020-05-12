// VeriBlock Blockchain Project
// Copyright 2017-2018 VeriBlock, Inc
// Copyright 2018-2019 Xenios SEZC
// All rights reserved.
// https://www.veriblock.org
// Distributed under the MIT software license, see the accompanying
// file LICENSE or http://www.opensource.org/licenses/mit-license.php.

package org.veriblock.sdk.models;

public class PublicationData {
    private final long identifier;
    private final byte[] header;
    private final byte[] payoutInfo;
    private final byte[] contextInfo;

    public long getIdentifier() {
        return identifier;
    }

    public byte[] getHeader() {
        return header;
    }


    public byte[] getPayoutInfo() {
        return payoutInfo;
    }
    public byte[] getContextInfo() {
        return contextInfo;
    }

    public PublicationData(long identifier, byte[] header, byte[] payoutInfo, byte[] contextInfo) {
        this.identifier = identifier;
        this.header = header;
        this.contextInfo = contextInfo;
        this.payoutInfo = payoutInfo;
    }

}
