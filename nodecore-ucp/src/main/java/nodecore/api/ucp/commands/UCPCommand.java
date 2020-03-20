// VeriBlock NodeCore
// Copyright 2017-2020 Xenios SEZC
// All rights reserved.
// https://www.veriblock.org
// Distributed under the MIT software license, see the accompanying
// file LICENSE or http://www.opensource.org/licenses/mit-license.php.

package nodecore.api.ucp.commands;

import nodecore.api.ucp.arguments.UCPArgument;
import nodecore.api.ucp.commands.client.*;
import nodecore.api.ucp.commands.server.*;
import org.veriblock.core.types.Pair;

import java.util.ArrayList;

/**
 * UCP, or Universal Client Protocol, is a means for clients to communicate with a NodeCore instance over a raw socket
 * and get information regarding the current state of the blockchain and participate in a pool if a NodeCore operator
 * decides to operate one.
 */
public abstract class UCPCommand {
    public enum Command {

        // Commands that a UCP client sends to a UCP server
        MINING_AUTH(MiningAuth.class,
                new Pair<String, UCPArgument.UCPType>("request_id", UCPArgument.UCPType.REQUEST_ID),
                new Pair<String, UCPArgument.UCPType>("username", UCPArgument.UCPType.USERNAME),
                new Pair<String, UCPArgument.UCPType>("password", UCPArgument.UCPType.PASSWORD)),

        MINING_SUBSCRIBE(MiningSubscribe.class,
                new Pair<String, UCPArgument.UCPType>("request_id", UCPArgument.UCPType.REQUEST_ID),
                new Pair<String, UCPArgument.UCPType>("update_frequency_ms", UCPArgument.UCPType.FREQUENCY_MS)),

        MINING_SUBMIT(MiningSubmit.class,
                new Pair<String, UCPArgument.UCPType>("request_id", UCPArgument.UCPType.REQUEST_ID),
                new Pair<String, UCPArgument.UCPType>("job_id", UCPArgument.UCPType.JOB_ID),
                new Pair<String, UCPArgument.UCPType>("nTime", UCPArgument.UCPType.TIMESTAMP),
                new Pair<String, UCPArgument.UCPType>("nonce", UCPArgument.UCPType.NONCE),
                new Pair<String, UCPArgument.UCPType>("extra_nonce", UCPArgument.UCPType.EXTRA_NONCE)),

        MINING_UNSUBSCRIBE(MiningUnsubscribe.class,
                new Pair<String, UCPArgument.UCPType>("request_id", UCPArgument.UCPType.REQUEST_ID)),

        MINING_RESET_ACK(MiningResetACK.class,
                new Pair<String, UCPArgument.UCPType>("request_id", UCPArgument.UCPType.REQUEST_ID)),

        MINING_MEMPOOL_UPDATE_ACK(MiningMempoolUpdateACK.class,
                new Pair<String, UCPArgument.UCPType>("request_id", UCPArgument.UCPType.REQUEST_ID)),

        GET_STATUS(GetStatus.class,
                new Pair<String, UCPArgument.UCPType>("", UCPArgument.UCPType.REQUEST_ID)),

        GET_ADDRESS_BALANCE_AND_INDEX(GetAddressBalanceAndIndex.class,
                new Pair<String, UCPArgument.UCPType>("request_id", UCPArgument.UCPType.REQUEST_ID),
                new Pair<String, UCPArgument.UCPType>("address", UCPArgument.UCPType.ADDRESS)),


        GET_TRANSACTIONS_MATCHING_FILTER(GetTransactionsMatchingFilter.class,
                new Pair<String, UCPArgument.UCPType>("request_id", UCPArgument.UCPType.REQUEST_ID),
                new Pair<String, UCPArgument.UCPType>("bloom_filter", UCPArgument.UCPType.BLOOM_FILTER),
                new Pair<String, UCPArgument.UCPType>("start_block", UCPArgument.UCPType.BLOCK_INDEX),
                new Pair<String, UCPArgument.UCPType>("stop_block", UCPArgument.UCPType.BLOCK_INDEX)),


        GET_BLOCK_HEADERS(GetBlockHeaders.class,
                new Pair<String, UCPArgument.UCPType>("request_id", UCPArgument.UCPType.REQUEST_ID),
                new Pair<String, UCPArgument.UCPType>("start_block", UCPArgument.UCPType.BLOCK_INDEX),
                new Pair<String, UCPArgument.UCPType>("stop_block", UCPArgument.UCPType.BLOCK_INDEX)),

        SEND_TRANSACTION(SendTransaction.class,
                new Pair<String, UCPArgument.UCPType>("request_id", UCPArgument.UCPType.REQUEST_ID),
                new Pair<String, UCPArgument.UCPType>("raw_transaction_data", UCPArgument.UCPType.TRANSACTION_DATA)),



        // Commands that a UCP server sends to a UCP client
        CAPABILITIES(Capabilities.class,
                new Pair<String, UCPArgument.UCPType>("request_id", UCPArgument.UCPType.REQUEST_ID),
                new Pair<String, UCPArgument.UCPType>("capabilities", UCPArgument.UCPType.BITFLAG)),

        MINING_RESET(MiningReset.class,
                new Pair<String, UCPArgument.UCPType>("request_id", UCPArgument.UCPType.REQUEST_ID)),

        MINING_JOB(MiningJob.class,
                new Pair<String, UCPArgument.UCPType>("request_id", UCPArgument.UCPType.REQUEST_ID),
                new Pair<String, UCPArgument.UCPType>("job_id", UCPArgument.UCPType.JOB_ID),
                new Pair<String, UCPArgument.UCPType>("block_index", UCPArgument.UCPType.BLOCK_INDEX),
                new Pair<String, UCPArgument.UCPType>("block_version", UCPArgument.UCPType.BLOCK_VERSION),
                new Pair<String, UCPArgument.UCPType>("previous_block_hash", UCPArgument.UCPType.BLOCK_HASH),
                new Pair<String, UCPArgument.UCPType>("second_previous_block_hash", UCPArgument.UCPType.BLOCK_HASH),
                new Pair<String, UCPArgument.UCPType>("third_previous_block_hash", UCPArgument.UCPType.BLOCK_HASH),
                new Pair<String, UCPArgument.UCPType>("pool_address", UCPArgument.UCPType.ADDRESS),
                new Pair<String, UCPArgument.UCPType>("merkle_root", UCPArgument.UCPType.TOP_LEVEL_MERKLE_ROOT),
                new Pair<String, UCPArgument.UCPType>("timestamp", UCPArgument.UCPType.TIMESTAMP),
                new Pair<String, UCPArgument.UCPType>("difficulty", UCPArgument.UCPType.DIFFICULTY),
                new Pair<String, UCPArgument.UCPType>("mining_target", UCPArgument.UCPType.TARGET),
                new Pair<String, UCPArgument.UCPType>("ledger_hash", UCPArgument.UCPType.LEDGER_HASH),
                new Pair<String, UCPArgument.UCPType>("coinbase_txid", UCPArgument.UCPType.TRANSACTION_ID),
                new Pair<String, UCPArgument.UCPType>("pop_datastore_hash", UCPArgument.UCPType.POP_DATASTORE_HASH),
                new Pair<String, UCPArgument.UCPType>("miner_comment", UCPArgument.UCPType.MINER_COMMENT),
                new Pair<String, UCPArgument.UCPType>("pop_transaction_merkle_root", UCPArgument.UCPType.INTERMEDIATE_LEVEL_MERKLE_ROOT),
                new Pair<String, UCPArgument.UCPType>("normal_transaction_merkle_root", UCPArgument.UCPType.INTERMEDIATE_LEVEL_MERKLE_ROOT),
                new Pair<String, UCPArgument.UCPType>("extra_nonce_start", UCPArgument.UCPType.EXTRA_NONCE),
                new Pair<String, UCPArgument.UCPType>("extra_nonce_end", UCPArgument.UCPType.EXTRA_NONCE),
                new Pair<String, UCPArgument.UCPType>("intermediate_metapackage_hash", UCPArgument.UCPType.INTERMEDIATE_METAPACKAGE_HASH)),

        MINING_MEMPOOL_UPDATE(MiningMempoolUpdate.class,
                new Pair<String, UCPArgument.UCPType>("request_id", UCPArgument.UCPType.REQUEST_ID),
                new Pair<String, UCPArgument.UCPType>("job_id", UCPArgument.UCPType.JOB_ID),
                new Pair<String, UCPArgument.UCPType>("pop_transaction_merkle_root", UCPArgument.UCPType.INTERMEDIATE_LEVEL_MERKLE_ROOT),
                new Pair<String, UCPArgument.UCPType>("normal_transaction_merkle_root", UCPArgument.UCPType.INTERMEDIATE_LEVEL_MERKLE_ROOT),
                new Pair<String, UCPArgument.UCPType>("new_merkle_root", UCPArgument.UCPType.TOP_LEVEL_MERKLE_ROOT),
                new Pair<String, UCPArgument.UCPType>("intermediate_metapackage_hash", UCPArgument.UCPType.INTERMEDIATE_METAPACKAGE_HASH)),

        MINING_AUTH_SUCCESS(MiningAuthSuccess.class,
                new Pair<String, UCPArgument.UCPType>("request_id", UCPArgument.UCPType.REQUEST_ID)),

        MINING_AUTH_FAILURE(MiningAuthFailure.class,
                new Pair<String, UCPArgument.UCPType>("request_id", UCPArgument.UCPType.REQUEST_ID),
                new Pair<String, UCPArgument.UCPType>("reason", UCPArgument.UCPType.MESSAGE)),

        MINING_SUBSCRIBE_SUCCESS(MiningSubscribeSuccess.class,
                new Pair<String, UCPArgument.UCPType>("request_id", UCPArgument.UCPType.REQUEST_ID)),

        MINING_SUBSCRIBE_FAILURE(MiningSubscribeFailure.class,
                new Pair<String, UCPArgument.UCPType>("request_id", UCPArgument.UCPType.REQUEST_ID),
                new Pair<String, UCPArgument.UCPType>("reason", UCPArgument.UCPType.MESSAGE)),

        MINING_UNSUBSCRIBE_SUCCESS(MiningUnsubscribeSuccess.class,
                new Pair<String, UCPArgument.UCPType>("request_id", UCPArgument.UCPType.REQUEST_ID)),

        MINING_UNSUBSCRIBE_FAILURE(MiningUnsubscribeFailure.class,
                new Pair<String, UCPArgument.UCPType>("request_id", UCPArgument.UCPType.REQUEST_ID),
                new Pair<String, UCPArgument.UCPType>("reason", UCPArgument.UCPType.MESSAGE)),

        MINING_SUBMIT_SUCCESS(MiningSubmitSuccess.class,
                new Pair<String, UCPArgument.UCPType>("request_id", UCPArgument.UCPType.REQUEST_ID)),

        MINING_SUBMIT_FAILURE(MiningSubmitFailure.class,
                new Pair<String, UCPArgument.UCPType>("request_id", UCPArgument.UCPType.REQUEST_ID),
                new Pair<String, UCPArgument.UCPType>("reason", UCPArgument.UCPType.MESSAGE)),

        ADDRESS_BALANCE_AND_INDEX(AddressBalanceAndIndex.class,
                new Pair<String, UCPArgument.UCPType>("request_id", UCPArgument.UCPType.REQUEST_ID),
                new Pair<String, UCPArgument.UCPType>("address", UCPArgument.UCPType.ADDRESS),
                new Pair<String, UCPArgument.UCPType>("balance", UCPArgument.UCPType.BALANCE),
                new Pair<String, UCPArgument.UCPType>("index", UCPArgument.UCPType.SIGNATURE_INDEX),
                new Pair<String, UCPArgument.UCPType>("ledger_merkle_path", UCPArgument.UCPType.MERKLE_PATH)),

        TRANSACTIONS_MATCHING_FILTER(TransactionsMatchingFilter.class,
                new Pair<String, UCPArgument.UCPType>("request_id", UCPArgument.UCPType.REQUEST_ID),
                new Pair<String, UCPArgument.UCPType>("transactions_with_context", UCPArgument.UCPType.TRANSACTIONS_WITH_CONTEXT)),

        TRANSACTION_SENT(TransactionSent.class,
                new Pair<String, UCPArgument.UCPType>("request_id", UCPArgument.UCPType.REQUEST_ID),
                new Pair<String, UCPArgument.UCPType>("transaction_id", UCPArgument.UCPType.TRANSACTION_ID)),

        BLOCK_HEADERS(BlockHeaders.class,
                new Pair<String, UCPArgument.UCPType>("request_id", UCPArgument.UCPType.REQUEST_ID),
                new Pair<String, UCPArgument.UCPType>("block_headers", UCPArgument.UCPType.BLOCK_HEADER_LIST)),

        ERROR_TRANSACTION_INVALID(ErrorTransactionInvalid.class,
                new Pair<String, UCPArgument.UCPType>("request_id", UCPArgument.UCPType.REQUEST_ID),
                new Pair<String, UCPArgument.UCPType>("raw_transaction_data", UCPArgument.UCPType.TRANSACTION_DATA)),

        ERROR_RANGE_TOO_LONG(ErrorRangeTooLong.class,
                new Pair<String, UCPArgument.UCPType>("request_id", UCPArgument.UCPType.REQUEST_ID)),

        ERROR_RANGE_INVALID(ErrorRangeInvalid.class,
                new Pair<String, UCPArgument.UCPType>("request_id", UCPArgument.UCPType.REQUEST_ID)),

        ERROR_RANGE_NOT_AVAILABLE(ErrorRangeNotAvailable.class,
                new Pair<String, UCPArgument.UCPType>("request_id", UCPArgument.UCPType.REQUEST_ID)),

        ERROR_UNKNOWN_COMMAND(ErrorUnknownCommand.class,
                new Pair<String, UCPArgument.UCPType>("request_id", UCPArgument.UCPType.REQUEST_ID));

        private final ArrayList<Pair<String, UCPArgument.UCPType>> pattern;
        private final Class<? extends UCPCommand> implementingClass;

        @SafeVarargs
        Command(Class<? extends UCPCommand> implementation, Pair<String, UCPArgument.UCPType> ... arguments) {
            this.implementingClass = implementation;

            ArrayList<Pair<String, UCPArgument.UCPType>> args = new ArrayList<Pair<String, UCPArgument.UCPType>>();

            for (int i = 0; i < arguments.length; i++) {
                args.add(arguments[i]);
            }

            this.pattern = args;
        }

        public ArrayList<Pair<String, UCPArgument.UCPType>> getPattern() {
            return pattern;
        }

        public Class<? extends UCPCommand> getCommandImplementingClass() {
            return implementingClass;
        }
    }

    /**
     * The "equality" of two UCPCommand objects should be based on producing identical serialized results
     * @param o Object to compare
     * @return Whether the provided object is equivalent to this object
     */
    public boolean equals(Object o) {
        if (!(o instanceof UCPCommand)) {
            return false;
        }

        if (this.compileCommand().equals(((UCPCommand)(o)).compileCommand())) {
            return true;
        }
        return false;
    }

    @Override
    public int hashCode() {
        return compileCommand().hashCode();
    }

    public abstract String compileCommand();
}
