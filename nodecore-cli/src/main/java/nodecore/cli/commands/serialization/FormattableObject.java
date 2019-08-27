// VeriBlock NodeCore CLI
// Copyright 2017-2019 Xenios SEZC
// All rights reserved.
// https://www.veriblock.org
// Distributed under the MIT software license, see the accompanying
// file LICENSE or http://www.opensource.org/licenses/mit-license.php.

package nodecore.cli.commands.serialization;

import com.google.gson.annotations.SerializedName;
import nodecore.api.grpc.Result;

import java.util.ArrayList;
import java.util.List;

public class FormattableObject<T> {
    public FormattableObject(final List<Result> results) {
        for (Result result : results)
            messages.add(new MessageInfo(result.getCode(),
                    result.getMessage(),
                    result.getDetails(),
                    result.getError()));
    }

    public static class MessageInfo {
        public MessageInfo(String code, String message, String details, boolean error) {
            this.code = code;
            this.message = message;
            this.details = details;
            this.isError = error;
        }
        public String code;

        public String message;

        public String details;

        @SerializedName("is_error")
        public boolean isError;
    }

    public T payload;

    public boolean success;

    public List<MessageInfo> messages = new ArrayList<>();
}
