// VeriBlock Blockchain Project
// Copyright 2017-2018 VeriBlock, Inc
// Copyright 2018-2020 Xenios SEZC
// All rights reserved.
// https://www.veriblock.org
// Distributed under the MIT software license, see the accompanying
// file LICENSE or http://www.opensource.org/licenses/mit-license.php.

package veriblock.model;

import java.util.List;
import java.util.concurrent.Executor;

import static com.google.common.base.Preconditions.checkNotNull;

public class ListenerRegistration<T> {
    public final T listener;
    public final Executor executor;

    public ListenerRegistration(T listener, Executor executor) {
        this.listener = listener;
        this.executor = executor;
    }

    public static <T> boolean removeFromList(T listener, List<? extends ListenerRegistration<T>> list) {
        checkNotNull(listener);

        ListenerRegistration<T> item = null;
        for (ListenerRegistration<T> registration : list) {
            if (registration.listener == listener) {
                item = registration;
                break;
            }
        }
        return item != null && list.remove(item);
    }
}
