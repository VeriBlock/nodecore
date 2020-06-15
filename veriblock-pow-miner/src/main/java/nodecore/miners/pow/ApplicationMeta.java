// VeriBlock PoW CPU Miner
// Copyright 2017-2020 Xenios SEZC
// All rights reserved.
// https://www.veriblock.org
// Distributed under the MIT software license, see the accompanying
// file LICENSE or http://www.opensource.org/licenses/mit-license.php.
package nodecore.miners.pow;

public class ApplicationMeta {
    public static final String PROGRAM_NAME = ApplicationMeta.class.getPackage().getSpecificationTitle();
    public static final String PROGRAM_VERSION = ApplicationMeta.class.getPackage().getImplementationVersion();
    public static final String FULL_PROGRAM_NAME_VERSION = PROGRAM_NAME + " v" + PROGRAM_VERSION;
}
