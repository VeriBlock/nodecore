// VeriBlock NodeCore
// Copyright 2017-2020 Xenios SEZC
// All rights reserved.
// https://www.veriblock.org
// Distributed under the MIT software license, see the accompanying
// file LICENSE or http://www.opensource.org/licenses/mit-license.php.

package org.veriblock.core;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.veriblock.core.params.NetworkParameters;

import java.util.concurrent.locks.ReentrantLock;

public class Context {
    private static final Logger logger = LoggerFactory.getLogger(Context.class);
    private static ReentrantLock lock = new ReentrantLock(true);
    private static Context instance;

    private final NetworkParameters networkParameters;
    public NetworkParameters getNetworkParameters() {
        return networkParameters;
    }

    private Context(NetworkParameters networkParameters) {
        this.networkParameters = networkParameters;
    }

    public static Context get() {
        if (instance == null) throw new IllegalStateException("Context is being accessed without having been initialized");

        return instance;
    }

    public static void create(NetworkParameters networkParameters) {
        lock.lock();
        try {
            if (instance != null) {
                logger.warn("Context can only be initialized once");
            } else {
                instance = new Context(networkParameters);
            }
        } finally {
            lock.unlock();
        }
    }
}
