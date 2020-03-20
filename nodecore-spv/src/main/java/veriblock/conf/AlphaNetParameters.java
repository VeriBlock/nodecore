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
 * Config for alfa net.
 */
public class AlphaNetParameters extends NetworkParameters {
    private static final String alphaNetworkGenesisBlock = "AAAAAAABAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAzyAl7A64qKMlSV/rWVALUFx5fAIEBfXhAw0E3w==";
    public static final String NETWORK = "alphanet";

    private AlphaNetParameters(){
        this.adminHost = LOCALHOST;
        this.adminPort = 10502;
        this.p2pPort = 7502;
        this.databaseName = "database-alpha.sqlite";
    }

    @Override
    public String getNetworkName() {
        return NETWORK;
    }

    @Override
    public VeriBlockBlock getGenesisBlock() {
        return SerializeDeserializeService.parseVeriBlockBlock(Base64.getDecoder().decode(alphaNetworkGenesisBlock));
    }

    @Override
    public BitcoinBlock getBitcoinOriginBlock() {
        return new BitcoinBlock(
                536870912,
                Sha256Hash.wrap("00000000000000b345b7bbf29bda1507a679b97967f99a10ab0088899529def7"),
                Sha256Hash.wrap("5e16e6cef738a2eba1fe7409318e3f558bec325392427aa3d8eaf46b028654f8"),
                Integer.parseUnsignedInt("1555501858"),
                Integer.parseUnsignedInt("436279940"),
                Integer.parseUnsignedInt("2599551022"));
    }

    @Override
    public Integer getProtocolVersion() {
        return 3;
    }


    private static AlphaNetParameters instance;

    public static synchronized AlphaNetParameters get() {
        if (instance == null) {
            instance = new AlphaNetParameters();
        }
        return instance;
    }

}
