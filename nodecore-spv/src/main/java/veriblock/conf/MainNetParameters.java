// VeriBlock Blockchain Project
// Copyright 2017-2018 VeriBlock, Inc
// Copyright 2018-2020 Xenios SEZC
// All rights reserved.
// https://www.veriblock.org
// Distributed under the MIT software license, see the accompanying
// file LICENSE or http://www.opensource.org/licenses/mit-license.php.
package veriblock.conf;

import org.veriblock.sdk.models.BitcoinBlock;
import org.veriblock.sdk.models.Sha256Hash;
import org.veriblock.sdk.models.VeriBlockBlock;
import org.veriblock.sdk.services.SerializeDeserializeService;

import java.util.Base64;

/**
 * Config for main net.
 */
public class MainNetParameters extends NetworkParameters {
    private static final String genesisBlock = "AAAAAAACAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAp+Xyt+yUKRdntNZ7SjNoLVyYfgsGAOjUET2FTQ==";
    public static final String NETWORK = "mainnet";

    private MainNetParameters() {
        this.adminHost = LOCALHOST;
        this.adminPort = 10500;
        this.p2pPort = 7500;
        this.bootstrapDns = "seed.veriblock.org";
        this.databaseName = "database.sqlite";
    }

    @Override
    public String getNetworkName() {
        return NETWORK;
    }

    @Override
    public VeriBlockBlock getGenesisBlock() {
        return SerializeDeserializeService.parseVeriBlockBlock(Base64.getDecoder().decode(genesisBlock));
    }

    @Override
    public BitcoinBlock getBitcoinOriginBlock() {
        return new BitcoinBlock(
                545259520,
                Sha256Hash.wrap("00000000000000000018f62f3a9fbec9bce00dca759407649d0ac2eaee34e45e"),
                Sha256Hash.wrap("11a29ab555186bde5ad5b20c54a3dc176ef9105a066df69934dcfe22f09c0984"),
                Integer.parseUnsignedInt("1553493015"),
                Integer.parseUnsignedInt("388767596"),
                Integer.parseUnsignedInt("2328158480"));
    }

    @Override
    public Integer getProtocolVersion() {
        return 3;
    }

    private static MainNetParameters instance;
    public static synchronized MainNetParameters get() {
        if (instance == null) {
            instance = new MainNetParameters();
        }
        return instance;
    }

}
