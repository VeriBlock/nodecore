// VeriBlock NodeCore
// Copyright 2017-2020 Xenios SEZC
// All rights reserved.
// https://www.veriblock.org
// Distributed under the MIT software license, see the accompanying
// file LICENSE or http://www.opensource.org/licenses/mit-license.php.
package nodecore.api.ucp.commands.server;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import nodecore.api.ucp.arguments.UCPArgument;
import nodecore.api.ucp.arguments.UCPArgumentBlockIndex;
import nodecore.api.ucp.arguments.UCPArgumentBloomFilter;
import nodecore.api.ucp.arguments.UCPArgumentRequestID;
import nodecore.api.ucp.commands.UCPCommand;
import nodecore.api.ucp.commands.UCPServerCommand;
import org.veriblock.core.crypto.BloomFilter;
import org.veriblock.core.types.Pair;

import java.util.ArrayList;

public class GetTransactionsMatchingFilter extends UCPServerCommand {
    private final UCPCommand.Command command = Command.GET_TRANSACTIONS_MATCHING_FILTER; // Not static for serialization purposes

    // Required
    private final UCPArgumentRequestID request_id;
    private final UCPArgumentBloomFilter bloom_filter;
    private final UCPArgumentBlockIndex start_block;
    private final UCPArgumentBlockIndex stop_block;


    public GetTransactionsMatchingFilter(UCPArgument ... arguments) {
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
        this.bloom_filter = (UCPArgumentBloomFilter) arguments[1];
        this.start_block = (UCPArgumentBlockIndex) arguments[2];
        this.stop_block = (UCPArgumentBlockIndex) arguments[3];
    }

    public GetTransactionsMatchingFilter(int request_id, String bloom_filter, int start_block, int stop_block) {
        this.request_id = new UCPArgumentRequestID(request_id);
        this.bloom_filter = new UCPArgumentBloomFilter(bloom_filter);
        this.start_block = new UCPArgumentBlockIndex(start_block);
        this.stop_block = new UCPArgumentBlockIndex(stop_block);
    }

    public static GetTransactionsMatchingFilter reconstitute(String commandLine) {
        if (commandLine == null) {
            throw new IllegalArgumentException(new Exception().getStackTrace()[0].getClassName() + "'s reconstitute cannot be called with a null commandLine!");
        }

        GsonBuilder deserializer = new GsonBuilder();
        deserializer.registerTypeAdapter(GetTransactionsMatchingFilter.class, new GetTransactionsMatchingFilterDeserializer());
        return deserializer.create().fromJson(commandLine, GetTransactionsMatchingFilter.class);
    }

    public int getRequestId() {
        return request_id.getData();
    }

    public int getStartBlock() {
        return start_block.getData();
    }

    public int getStopBlock() {
        return stop_block.getData();
    }

    public String compileCommand() { return new Gson().toJson(this); }
}
