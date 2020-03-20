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
 * Config for test net.
 */
public class TestNetParameters extends NetworkParameters {

    public static final String GENESIS_BLOCK = "AAAAAAACAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAoup8Ke95FdtBLr1AEqnGF12eNUgEBfXhANpFeQ==";
    public static final String NETWORK = "testnet";

    private TestNetParameters(){
        this.adminHost = LOCALHOST;
        this.adminPort = 10501;
        this.p2pPort = 7501;
        this.bootstrapDns = "seedtestnet.veriblock.org";
        this.databaseName = "database-test.sqlite";
    }

    @Override
    public String getNetworkName() {
        return NETWORK;
    }

    @Override
    public BitcoinBlock getBitcoinOriginBlock() {
        return new BitcoinBlock(
        536870912,
                Sha256Hash.wrap("00000000251E9261A15339B4BF0540A44328EC83F3797B9BAC67F47558D5F14E"),
                Sha256Hash.wrap("CBF519E1DC00F8FFBDC31A6AC3A73109D95890EDD9283EA71AD9BE11639249E9"),
                Integer.parseUnsignedInt("1570648139"),
                Integer.parseUnsignedInt("486604799"),
                Integer.parseUnsignedInt("203968315"));

    }

    @Override
    public Integer getProtocolVersion() {
        return 2;
    }

    @Override
    public VeriBlockBlock getGenesisBlock() {
        return SerializeDeserializeService.parseVeriBlockBlock(Base64.getDecoder().decode(GENESIS_BLOCK));
    }

    private static TestNetParameters instance;

    public static synchronized TestNetParameters get() {
        if (instance == null) {
            instance = new TestNetParameters();
        }
        return instance;
    }
}
