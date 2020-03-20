// VeriBlock NodeCore
// Copyright 2017-2020 Xenios SEZC
// All rights reserved.
// https://www.veriblock.org
// Distributed under the MIT software license, see the accompanying
// file LICENSE or http://www.opensource.org/licenses/mit-license.php.

package nodecore.api.ucp.commands.client;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import nodecore.api.ucp.arguments.UCPArgument;
import nodecore.api.ucp.arguments.UCPArgumentRequestID;
import nodecore.api.ucp.arguments.UCPArgumentTransactionsWithContext;
import nodecore.api.ucp.commands.UCPClientCommand;
import nodecore.api.ucp.commands.UCPCommand;
import org.veriblock.core.types.Pair;

import java.util.ArrayList;

// TODO: Update once transaction context scaffolding has been added; this can be done after phase-1 launch to support thin clients
public class TransactionsMatchingFilter extends UCPClientCommand {
    private final UCPCommand.Command command = Command.TRANSACTIONS_MATCHING_FILTER; // Not static for serialization purposes

    // Required
    private final UCPArgumentRequestID request_id;
    private final UCPArgumentTransactionsWithContext transactions_with_context;

    public TransactionsMatchingFilter(UCPArgument ... arguments) {
        ArrayList<Pair<String, UCPArgument.UCPType>> pattern = command.getPattern();

        if (arguments.length != pattern.size()) {
            throw new IllegalArgumentException(getClass().getCanonicalName() + "'s constructor cannot be called without exactly " + pattern.size() + " UCPArguments!");
        }

        for (int i = 0; i < pattern.size(); i++) {
            if (arguments[i].getType() != pattern.get(i).getSecond()) {
                throw new IllegalArgumentException(getClass().getCanonicalName()
                        + "'s constructor cannot be called with a argument at index "
                        + i + " which is a " + arguments[i].getType()
                        + " instead of a " + pattern.get(i).getSecond() + "!");
            }
        }

        this.request_id = (UCPArgumentRequestID)arguments[0];
        this.transactions_with_context = (UCPArgumentTransactionsWithContext)arguments[1];
    }

    public TransactionsMatchingFilter(int request_id, String transactions_with_context) {
        this.request_id = new UCPArgumentRequestID(request_id);
        this.transactions_with_context = new UCPArgumentTransactionsWithContext(transactions_with_context);
    }

    public static TransactionsMatchingFilter reconstitute(String commandLine) {
        if (commandLine == null) {
            throw new IllegalArgumentException(new Exception().getStackTrace()[0].getClassName() + "'s reconstitute cannot be called with a null commandLine!");
        }

        GsonBuilder deserializer = new GsonBuilder();
        deserializer.registerTypeAdapter(TransactionsMatchingFilter.class, new TransactionsMatchingFilterDeserializer());
        return deserializer.create().fromJson(commandLine, TransactionsMatchingFilter.class);
    }

    public int getRequestId() {
        return request_id.getData();
    }

    public String getTransactionsWithContext() {
        return transactions_with_context.getData();
    }

    public String compileCommand() { return new Gson().toJson(this); }
}
