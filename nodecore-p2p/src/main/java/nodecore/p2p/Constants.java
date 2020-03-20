// VeriBlock NodeCore
// Copyright 2017-2020 Xenios SEZC
// All rights reserved.
// https://www.veriblock.org
// Distributed under the MIT software license, see the accompanying
// file LICENSE or http://www.opensource.org/licenses/mit-license.php.

package nodecore.p2p;

public final class Constants {

    public static final String PROGRAM_NAME = Constants.class.getPackage().getSpecificationTitle();
    public static final String PROGRAM_VERSION = Constants.class.getPackage().getImplementationVersion();
    public static final String FULL_PROGRAM_NAME_VERSION = PROGRAM_NAME + " v" + PROGRAM_VERSION;
    public static final String PLATFORM = System.getProperty("os.name") + " | " + System.getProperty("java.version");

    public static final int PEER_TIMEOUT = 300;
    public static final int PEER_REQUEST_TIMEOUT = 15;
    public static final int BLOCKCHAIN_STALE_UPDATE_PERIOD_MS = 15 * 1000; // 15 seconds
    public static final int PEER_ACKNOWLEDGE_THRESHOLD = 3;
    public static final int PEER_MAX_ADVERTISEMENTS = 50000;
    public static final int PEER_MESSAGE_SIZE_LIMIT = 1024 * 1024 * 4; // 4 MB
    public static final int PEER_BAN_MESSAGE_SIZE_LIMIT = 1024 * 1024 * 16; // 16 MB
}
