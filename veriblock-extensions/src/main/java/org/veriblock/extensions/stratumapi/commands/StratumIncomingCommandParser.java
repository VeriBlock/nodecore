package org.veriblock.extensions.stratumapi.commands;

import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import com.google.gson.JsonPrimitive;
import com.google.gson.JsonSyntaxException;
import nodecore.api.ucp.commands.UCPClientCommand;
import nodecore.api.ucp.commands.UCPCommand;
import nodecore.api.ucp.commands.client.AddressBalanceAndIndex;
import nodecore.api.ucp.commands.client.BlockHeaders;
import nodecore.api.ucp.commands.client.Capabilities;
import nodecore.api.ucp.commands.client.ErrorRangeInvalid;
import nodecore.api.ucp.commands.client.ErrorRangeNotAvailable;
import nodecore.api.ucp.commands.client.ErrorRangeTooLong;
import nodecore.api.ucp.commands.client.ErrorTransactionInvalid;
import nodecore.api.ucp.commands.client.ErrorUnknownCommand;
import nodecore.api.ucp.commands.client.MiningAuthFailure;
import nodecore.api.ucp.commands.client.MiningAuthSuccess;
import nodecore.api.ucp.commands.client.MiningJob;
import nodecore.api.ucp.commands.client.MiningMempoolUpdate;
import nodecore.api.ucp.commands.client.MiningReset;
import nodecore.api.ucp.commands.client.MiningSubmitFailure;
import nodecore.api.ucp.commands.client.MiningSubmitSuccess;
import nodecore.api.ucp.commands.client.MiningSubscribeFailure;
import nodecore.api.ucp.commands.client.MiningSubscribeSuccess;
import nodecore.api.ucp.commands.client.MiningUnsubscribeFailure;
import nodecore.api.ucp.commands.client.MiningUnsubscribeSuccess;
import nodecore.api.ucp.commands.client.TransactionSent;
import nodecore.api.ucp.commands.client.TransactionsMatchingFilter;
import org.veriblock.extensions.stratumapi.commands.toserver.MiningAuthorize;
import org.veriblock.extensions.stratumapi.commands.toserver.MiningExtraNonceSubscribe;
import org.veriblock.extensions.stratumapi.commands.toserver.MiningHello;
import org.veriblock.extensions.stratumapi.commands.toserver.MiningSubmit;
import org.veriblock.extensions.stratumapi.commands.toserver.MiningSubscribe;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class StratumIncomingCommandParser {
    private static final Logger _logger = LoggerFactory.getLogger(StratumIncomingCommandParser.class);

    // TODO: remove
    public static void main(String[] args) {
        String toParse = "{\"id\":1,\"method\":\"mining.hello\",\"params\":{\"agent\":\"ethminer-0.18.0\",\"host\":\"eth.2miners.com\",\"port\":\"7e4\",\"proto\":\"EthereumStratum/2.0.0\"}}";
        StratumServerCommand parsed = parseServerCommand(toParse);
        System.out.println("Original" + toParse);
        System.out.println("Final: " + parsed.compileCommand());
    }

    public static StratumServerCommand parseServerCommand(String commandLine) throws InvalidStratumCommandException {
        if (commandLine == null) {
            throw new IllegalArgumentException("parseServerCommand cannot be called with a null command line String!");
        }

        if (commandLine.length() == 0) {
            throw new IllegalArgumentException("parseServerCommand cannot be called with a zero-length command line String!");
        }

        String methodName = null;
        String resultType = null;
        try {
            _logger.info("Attempting to parse command line: =@@=" + commandLine + "=@@=");
            JsonElement element = new JsonParser().parse(commandLine);
            JsonPrimitive methodElement = element.getAsJsonObject().getAsJsonPrimitive("method");
            JsonPrimitive resultElement = element.getAsJsonObject().getAsJsonPrimitive("result");
            if (methodElement != null) {
                methodName = methodElement.getAsString();
            } else if (resultElement != null) {
                resultType = resultElement.getAsString();
            }
            else {
                _logger.error("Invalid Stratum Command/Result message: {}", commandLine);
                throw new InvalidStratumCommandException();
            }

            if (methodName != null && methodName.equalsIgnoreCase(StratumCommand.Command.MINING_HELLO.getFriendlyName())) {
                return MiningHello.reconstitute(element);
            } else if (methodName != null && methodName.equalsIgnoreCase(StratumCommand.Command.MINING_SUBSCRIBE.getFriendlyName())) {
                return MiningSubscribe.reconstitute(element);
            } else if (methodName != null && methodName.equalsIgnoreCase(StratumCommand.Command.MINING_EXTRA_NONCE_SUBSCRIBE.getFriendlyName())) {
                return MiningExtraNonceSubscribe.reconstitute(element);
            } else if (methodName != null && methodName.equalsIgnoreCase(StratumCommand.Command.MINING_AUTHORIZE.getFriendlyName())) {
                return MiningAuthorize.reconstitute(element);
            } else if (methodName != null && methodName.equalsIgnoreCase(StratumCommand.Command.MINING_SUBMIT.getFriendlyName())) {
                return MiningSubmit.reconstitute(element);
            }
        } catch (JsonSyntaxException e) {
            _logger.error("Invalid Json Syntax in message {}", commandLine, e);
            throw new InvalidStratumCommandException();
        }

        throw new IllegalArgumentException("parseServerCommand was called with an unrecognized commandLine (" + commandLine + ")!");
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
            } else if (commandName.equals(UCPCommand.Command.BLOCK_HEADERS.name().toUpperCase())) {
                return BlockHeaders.reconstitute(commandLine);
            } else if (commandName.equals(UCPCommand.Command.CAPABILITIES.name().toUpperCase())) {
                return Capabilities.reconstitute(commandLine);
            } else if (commandName.equals(UCPCommand.Command.ERROR_RANGE_INVALID.name().toUpperCase())) {
                return ErrorRangeInvalid.reconstitute(commandLine);
            } else if (commandName.equals(UCPCommand.Command.ERROR_RANGE_NOT_AVAILABLE.name().toUpperCase())) {
                return ErrorRangeNotAvailable.reconstitute(commandLine);
            } else if (commandName.equals(UCPCommand.Command.ERROR_RANGE_TOO_LONG.name().toUpperCase())) {
                return ErrorRangeTooLong.reconstitute(commandLine);
            } else if (commandName.equals(UCPCommand.Command.ERROR_TRANSACTION_INVALID.name().toUpperCase())) {
                return ErrorTransactionInvalid.reconstitute(commandLine);
            } else if (commandName.equals(UCPCommand.Command.ERROR_UNKNOWN_COMMAND.name().toUpperCase())) {
                return ErrorUnknownCommand.reconstitute(commandLine);
            } else if (commandName.equals(UCPCommand.Command.MINING_AUTH_FAILURE.name().toUpperCase())) {
                return MiningAuthFailure.reconstitute(commandLine);
            } else if (commandName.equals(UCPCommand.Command.MINING_AUTH_SUCCESS.name().toUpperCase())) {
                return MiningAuthSuccess.reconstitute(commandLine);
            } else if (commandName.equals(UCPCommand.Command.MINING_JOB.name().toUpperCase())) {
                return MiningJob.reconstitute(commandLine);
            } else if (commandName.equals(UCPCommand.Command.MINING_MEMPOOL_UPDATE.name().toUpperCase())) {
                return MiningMempoolUpdate.reconstitute(commandLine);
            } else if (commandName.equals(UCPCommand.Command.MINING_RESET.name().toUpperCase())) {
                return MiningReset.reconstitute(commandLine);
            } else if (commandName.equals(UCPCommand.Command.MINING_SUBMIT_FAILURE.name().toUpperCase())) {
                return MiningSubmitFailure.reconstitute(commandLine);
            } else if (commandName.equals(UCPCommand.Command.MINING_SUBMIT_SUCCESS.name().toUpperCase())) {
                return MiningSubmitSuccess.reconstitute(commandLine);
            } else if (commandName.equals(UCPCommand.Command.MINING_SUBSCRIBE_FAILURE.name().toUpperCase())) {
                return MiningSubscribeFailure.reconstitute(commandLine);
            } else if (commandName.equals(UCPCommand.Command.MINING_SUBSCRIBE_SUCCESS.name().toUpperCase())) {
                return MiningSubscribeSuccess.reconstitute(commandLine);
            } else if (commandName.equals(UCPCommand.Command.MINING_UNSUBSCRIBE_FAILURE.name().toUpperCase())) {
                return MiningUnsubscribeFailure.reconstitute(commandLine);
            } else if (commandName.equals(UCPCommand.Command.MINING_UNSUBSCRIBE_SUCCESS.name().toUpperCase())) {
                return MiningUnsubscribeSuccess.reconstitute(commandLine);
            } else if (commandName.equals(UCPCommand.Command.TRANSACTION_SENT.name().toUpperCase())) {
                return TransactionSent.reconstitute(commandLine);
            } else if (commandName.equals(UCPCommand.Command.TRANSACTIONS_MATCHING_FILTER.name().toUpperCase())) {
                return TransactionsMatchingFilter.reconstitute(commandLine);
            }
        } catch (Exception e) {
            _logger.error("An error was encountered while parsing a commandLine request: " + commandLine + "!");
            return null;
        }

        throw new IllegalArgumentException("parseServerCommand was called with an unrecognized commandLine (" + commandLine + ")!");
    }
}
