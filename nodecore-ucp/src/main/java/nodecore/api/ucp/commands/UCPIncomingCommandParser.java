// VeriBlock NodeCore
// Copyright 2017-2020 Xenios SEZC
// All rights reserved.
// https://www.veriblock.org
// Distributed under the MIT software license, see the accompanying
// file LICENSE or http://www.opensource.org/licenses/mit-license.php.

package nodecore.api.ucp.commands;

import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import com.google.gson.JsonPrimitive;
import com.google.gson.JsonSyntaxException;
import nodecore.api.ucp.commands.client.*;
import nodecore.api.ucp.commands.server.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class UCPIncomingCommandParser {
    private static final Logger _logger = LoggerFactory.getLogger(UCPIncomingCommandParser.class);

    public static UCPServerCommand parseServerCommand(String commandLine) throws InvalidUCPCommandException {
        if (commandLine == null) {
            throw new IllegalArgumentException("parseServerCommand cannot be called with a null command line String!");
        }

        if (commandLine.length() == 0) {
            throw new IllegalArgumentException("parseServerCommand cannot be called with a zero-length command line String!");
        }

        String commandName;
        try {
            JsonElement element = new JsonParser().parse(commandLine);
            JsonPrimitive commandElement = element.getAsJsonObject().getAsJsonPrimitive("command");
            if (commandElement == null) {
                _logger.error("Invalid UCP Command message: {}", commandLine);
                throw new InvalidUCPCommandException();
            }

            commandName = commandElement.getAsString().toUpperCase();
        } catch (JsonSyntaxException e) {
            _logger.error("Invalid Json Syntax in message", commandLine, e);
            throw new InvalidUCPCommandException();
        }

        try {
            if (commandName.equals(UCPCommand.Command.GET_ADDRESS_BALANCE_AND_INDEX.name().toUpperCase())) {
                return GetAddressBalanceAndIndex.reconstitute(commandLine);
            }

            else if (commandName.equals(UCPCommand.Command.GET_BLOCK_HEADERS.name().toUpperCase())) {
                return GetBlockHeaders.reconstitute(commandLine);
            }

            else if (commandName.equals(UCPCommand.Command.GET_TRANSACTIONS_MATCHING_FILTER.name().toUpperCase())) {
                return GetTransactionsMatchingFilter.reconstitute(commandLine);
            }

            else if (commandName.equals(UCPCommand.Command.GET_STATUS.name().toUpperCase())) {
                return GetStatus.reconstitute(commandLine);
            }

            else if (commandName.equals(UCPCommand.Command.MINING_AUTH.name().toUpperCase())) {
                return MiningAuth.reconstitute(commandLine);
            }

            else if (commandName.equals(UCPCommand.Command.MINING_MEMPOOL_UPDATE_ACK.name().toUpperCase())) {
                return MiningMempoolUpdateACK.reconstitute(commandLine);
            }

            else if (commandName.equals(UCPCommand.Command.MINING_RESET_ACK.name().toUpperCase())) {
                return MiningResetACK.reconstitute(commandLine);
            }

            else if (commandName.equals(UCPCommand.Command.MINING_SUBMIT.name().toUpperCase())) {
                return MiningSubmit.reconstitute(commandLine);
            }

            else if (commandName.equals(UCPCommand.Command.MINING_SUBSCRIBE.name().toUpperCase())) {
                return MiningSubscribe.reconstitute(commandLine);
            }

            else if (commandName.equals(UCPCommand.Command.MINING_UNSUBSCRIBE.name().toUpperCase())) {
                return MiningUnsubscribe.reconstitute(commandLine);
            }

            else if (commandName.equals(UCPCommand.Command.SEND_TRANSACTION.name().toUpperCase())) {
                return SendTransaction.reconstitute(commandLine);
            }

        } catch (Exception e) {
            _logger.error("An error was encountered while parsing a commandLine request: {}", commandLine);
            throw new InvalidUCPCommandException();
        }

        _logger.error("parseServerCommand was called with an unrecognized commandLine: {}", commandLine);
        throw new InvalidUCPCommandException();
    }


    public static UCPClientCommand parseClientCommand(String commandLine) {
        if (commandLine == null) {
            throw new IllegalArgumentException("parseClientCommand cannot be called with a null command line String!");
        }

        if (commandLine.length() == 0) {
            throw new IllegalArgumentException("parseClientCommand cannot be called with a zero-length command line String!");
        }

        try {
            JsonElement element = new JsonParser().parse(commandLine);
            String commandName = element.getAsJsonObject().getAsJsonPrimitive("command").getAsString().toUpperCase();

            if (commandName.equals(UCPCommand.Command.ADDRESS_BALANCE_AND_INDEX.name().toUpperCase())) {
                return AddressBalanceAndIndex.reconstitute(commandLine);
            }

            else if (commandName.equals(UCPCommand.Command.BLOCK_HEADERS.name().toUpperCase())) {
                return BlockHeaders.reconstitute(commandLine);
            }

            else if (commandName.equals(UCPCommand.Command.CAPABILITIES.name().toUpperCase())) {
                return Capabilities.reconstitute(commandLine);
            }

            else if (commandName.equals(UCPCommand.Command.ERROR_RANGE_INVALID.name().toUpperCase())) {
                return ErrorRangeInvalid.reconstitute(commandLine);
            }

            else if (commandName.equals(UCPCommand.Command.ERROR_RANGE_NOT_AVAILABLE.name().toUpperCase())) {
                return ErrorRangeNotAvailable.reconstitute(commandLine);
            }

            else if (commandName.equals(UCPCommand.Command.ERROR_RANGE_TOO_LONG.name().toUpperCase())) {
                return ErrorRangeTooLong.reconstitute(commandLine);
            }

            else if (commandName.equals(UCPCommand.Command.ERROR_TRANSACTION_INVALID.name().toUpperCase())) {
                return ErrorTransactionInvalid.reconstitute(commandLine);
            }

            else if (commandName.equals(UCPCommand.Command.ERROR_UNKNOWN_COMMAND.name().toUpperCase())) {
                return ErrorUnknownCommand.reconstitute(commandLine);
            }

            else if (commandName.equals(UCPCommand.Command.MINING_AUTH_FAILURE.name().toUpperCase())) {
                return MiningAuthFailure.reconstitute(commandLine);
            }

            else if (commandName.equals(UCPCommand.Command.MINING_AUTH_SUCCESS.name().toUpperCase())) {
                return MiningAuthSuccess.reconstitute(commandLine);
            }

            else if (commandName.equals(UCPCommand.Command.MINING_JOB.name().toUpperCase())) {
                return MiningJob.reconstitute(commandLine);
            }

            else if (commandName.equals(UCPCommand.Command.MINING_MEMPOOL_UPDATE.name().toUpperCase())) {
                return MiningMempoolUpdate.reconstitute(commandLine);
            }

            else if (commandName.equals(UCPCommand.Command.MINING_RESET.name().toUpperCase())) {
                return MiningReset.reconstitute(commandLine);
            }

            else if (commandName.equals(UCPCommand.Command.MINING_SUBMIT_FAILURE.name().toUpperCase())) {
                return MiningSubmitFailure.reconstitute(commandLine);
            }

            else if (commandName.equals(UCPCommand.Command.MINING_SUBMIT_SUCCESS.name().toUpperCase())) {
                return MiningSubmitSuccess.reconstitute(commandLine);
            }

            else if (commandName.equals(UCPCommand.Command.MINING_SUBSCRIBE_FAILURE.name().toUpperCase())) {
                return MiningSubscribeFailure.reconstitute(commandLine);
            }

            else if (commandName.equals(UCPCommand.Command.MINING_SUBSCRIBE_SUCCESS.name().toUpperCase())) {
                return MiningSubscribeSuccess.reconstitute(commandLine);
            }

            else if (commandName.equals(UCPCommand.Command.MINING_UNSUBSCRIBE_FAILURE.name().toUpperCase())) {
                return MiningUnsubscribeFailure.reconstitute(commandLine);
            }

            else if (commandName.equals(UCPCommand.Command.MINING_UNSUBSCRIBE_SUCCESS.name().toUpperCase())) {
                return MiningUnsubscribeSuccess.reconstitute(commandLine);
            }

            else if (commandName.equals(UCPCommand.Command.TRANSACTION_SENT.name().toUpperCase())) {
                return TransactionSent.reconstitute(commandLine);
            }

            else if (commandName.equals(UCPCommand.Command.TRANSACTIONS_MATCHING_FILTER.name().toUpperCase())) {
                return TransactionsMatchingFilter.reconstitute(commandLine);
            }
        } catch (Exception e) {
            _logger.error("An error was encountered while parsing a commandLine request: " + commandLine + "!");
            return null;
        }

        throw new IllegalArgumentException("parseServerCommand was called with an unrecognized commandLine (" + commandLine + ")!");
    }




}
