package veriblock.model;

import nodecore.api.grpc.VeriBlockMessages;

import java.util.concurrent.atomic.AtomicBoolean;

public class FutureEventReply {

    private final AtomicBoolean isDone = new AtomicBoolean(false);
    private VeriBlockMessages.Event response;

    public synchronized void response(VeriBlockMessages.Event event) {
        this.response = event;
        this.isDone.set(true);
    }

    public boolean isDone() {
        return isDone.get();
    }

    public VeriBlockMessages.Event getResponse() {
        return response;
    }
}
