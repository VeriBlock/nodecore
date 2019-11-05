// VeriBlock PoP Miner
// Copyright 2017-2019 Xenios SEZC
// All rights reserved.
// https://www.veriblock.org
// Distributed under the MIT software license, see the accompanying
// file LICENSE or http://www.opensource.org/licenses/mit-license.php.

package nodecore.miners.pop.services;

import com.google.common.eventbus.Subscribe;
import nodecore.miners.pop.InternalEventBus;
import nodecore.miners.pop.contracts.MessageEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.CountDownLatch;

public class MessageService {
    private static final Logger logger = LoggerFactory.getLogger(MessageService.class);
    private final ConcurrentLinkedQueue<MessageEvent> queue;

    private CountDownLatch messageGate;
    private boolean running;

    public MessageService() {
        this.queue = new ConcurrentLinkedQueue<>();
        this.messageGate = new CountDownLatch(1);
        this.running = true;

        InternalEventBus.getInstance().register(this);
    }

    public List<MessageEvent> getMessages() {
        if (!running) return Collections.emptyList();

        try {
            messageGate.await();
        } catch (InterruptedException e) {
            logger.error(e.getMessage(), e);
        }

        List<MessageEvent> messages = new ArrayList<>();
        MessageEvent message;
        while ((message = queue.poll()) != null) {
            messages.add(message);
        }

        messageGate = new CountDownLatch(1);
        return messages;
    }

    public void shutdown() {
        this.running = false;
        messageGate.countDown();
    }


    @Subscribe public void onMessage(MessageEvent event) {
        try {
            queue.add(event);
            messageGate.countDown();
        } catch (Exception e) {
            logger.error(e.getMessage(), e);
        }
    }
}
