// VeriBlock NodeCore
// Copyright 2017-2020 Xenios SEZC
// All rights reserved.
// https://www.veriblock.org
// Distributed under the MIT software license, see the accompanying
// file LICENSE or http://www.opensource.org/licenses/mit-license.php.

package nodecore.api.ucp.commands.client;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import nodecore.api.ucp.arguments.*;
import nodecore.api.ucp.commands.UCPClientCommand;
import nodecore.api.ucp.commands.UCPCommand;
import org.veriblock.core.contracts.LedgerMerklePath;
import org.veriblock.core.types.Pair;

import java.util.ArrayList;

public class AddressBalanceAndIndex extends UCPClientCommand {
    private final UCPCommand.Command command = Command.ADDRESS_BALANCE_AND_INDEX; // Not static for serialization purposes

    // Required
    private final UCPArgumentRequestID request_id;
    private final UCPArgumentAddress address;
    private final UCPArgumentBalance balance;
    private final UCPArgumentSignatureIndex index;
    private final UCPArgumentLedgerMerklePath ledger_merkle_path;

    public AddressBalanceAndIndex(UCPArgument ... arguments) {
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
        this.address = (UCPArgumentAddress)arguments[1];
        this.balance = (UCPArgumentBalance)arguments[2];
        this.index = (UCPArgumentSignatureIndex)arguments[3];
        this.ledger_merkle_path = (UCPArgumentLedgerMerklePath)arguments[3];
    }

    public AddressBalanceAndIndex(int request_id, String address, long balance, long index, LedgerMerklePath ledger_merkle_path) {
        this.request_id = new UCPArgumentRequestID(request_id);
        this.address = new UCPArgumentAddress(address);
        this.balance = new UCPArgumentBalance(balance);
        this.index = new UCPArgumentSignatureIndex(index);
        this.ledger_merkle_path = new UCPArgumentLedgerMerklePath(ledger_merkle_path);
    }

    public AddressBalanceAndIndex(int request_id, String address, long balance, long index, String ledger_merkle_path) {
        this.request_id = new UCPArgumentRequestID(request_id);
        this.address = new UCPArgumentAddress(address);
        this.balance = new UCPArgumentBalance(balance);
        this.index = new UCPArgumentSignatureIndex(index);
        this.ledger_merkle_path = new UCPArgumentLedgerMerklePath(ledger_merkle_path);
    }

    public static AddressBalanceAndIndex reconstitute(String commandLine) {
        if (commandLine == null) {
            throw new IllegalArgumentException(new Exception().getStackTrace()[0].getClassName() + "'s reconstitute cannot be called with a null commandLine!");
        }

        GsonBuilder deserializer = new GsonBuilder();
        deserializer.registerTypeAdapter(AddressBalanceAndIndex.class, new AddressBalanceAndIndexDeserializer());
        return deserializer.create().fromJson(commandLine, AddressBalanceAndIndex.class);
    }

    public int getRequestId() {
        return request_id.getData();
    }

    public String getAddress() {
        return address.getData();
    }

    public long getBalance() {
        return balance.getData();
    }

    public LedgerMerklePath getTransactionInsertionList() {
        return ledger_merkle_path.getData();
    }

    public String compileCommand() { return new Gson().toJson(this); }
}
