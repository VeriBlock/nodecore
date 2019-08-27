// VeriBlock NodeCore CLI
// Copyright 2017-2019 Xenios SEZC
// All rights reserved.
// https://www.veriblock.org
// Distributed under the MIT software license, see the accompanying
// file LICENSE or http://www.opensource.org/licenses/mit-license.php.

package nodecore.cli.commands.rpc;

import com.google.inject.Inject;
import io.grpc.StatusRuntimeException;
import nodecore.api.grpc.*;
import nodecore.api.grpc.utilities.ByteStringAddressUtility;
import nodecore.cli.annotations.CommandParameterType;
import nodecore.cli.annotations.CommandSpec;
import nodecore.cli.annotations.CommandSpecParameter;
import nodecore.cli.commands.serialization.FormattableObject;
import nodecore.cli.commands.serialization.WalletTransactionInfo;
import nodecore.cli.contracts.Command;
import nodecore.cli.contracts.CommandContext;
import nodecore.cli.contracts.DefaultResult;
import nodecore.cli.contracts.Result;
import nodecore.cli.utilities.CommandUtility;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

@CommandSpec(
        name = "Get wallet transactions",
        form = "getwallettransactions",
        description = "Writes transaction history for an address to a local file. This could take a while.")
@CommandSpecParameter(name = "address", required = true, type = CommandParameterType.STANDARD_ADDRESS)
@CommandSpecParameter(name = "type", required = false, type = CommandParameterType.STRING)
public class GetWalletTransactionsCommand implements Command {
    private static final Logger _logger = LoggerFactory.getLogger(GetWalletTransactionsCommand.class);

    @Inject
    public GetWalletTransactionsCommand() {
    }

    private WalletTransaction.Type getTxType(String type)
    {
        WalletTransaction.Type transactionType;

        switch (type) {

            case "popcoinbase":
                transactionType = WalletTransaction.Type.POP_COINBASE;
                break;

            case "powcoinbase":
                transactionType = WalletTransaction.Type.POW_COINBASE;
                break;

            case "coinbase":
                transactionType = WalletTransaction.Type.BOTH_COINBASE;
                break;

            case "pop":
                transactionType = WalletTransaction.Type.POP;
                break;

            case "received":
                transactionType = WalletTransaction.Type.RECEIVED;
                break;

            case "sent":
                transactionType = WalletTransaction.Type.SENT;
                break;

            default:
                transactionType = WalletTransaction.Type.NOT_SET;
        }
        return transactionType;
    }

    @Override
    public Result execute(CommandContext context) throws Exception {

        int pageSize = 1000;

        Result result = new DefaultResult();

        String address = context.getParameter("address");
        String type = context.getParameter("type");
        String relativeFile = address + ".csv";

        try {
            String outputFile = (new File(relativeFile)).getCanonicalPath().toString();

            //Want to output this immediately so that user knows where the exact file is, and could monitor it
            context.outputStatus( String.format("Append to file: %1$s", outputFile) );

            WalletTransaction.Type transactionType;

            if (type == null) {
                transactionType = WalletTransaction.Type.NOT_SET;
            } else {
                transactionType = getTxType(type);
            }

            int pageNum = 1;
            boolean done = false;

            //Delete the file if it exists, in preparation for creating new file
            startFileHeader(outputFile);

            int totalCount = 0;
            while (!done) {

                //get the data
                GetWalletTransactionsReply reply = getTransactions(context, address, pageNum, pageSize, transactionType);

                try {
                    if (reply == null) {
                        result.fail();
                        done = true;
                    } else if (reply.getCacheState() != GetWalletTransactionsReply.CacheState.CURRENT) {
                        //bad
                        result.fail();
                        result.addMessage("-2", "Address CacheState not CURRENT", reply.getMessage(), true);
                        done = true;
                    } else {
                        int resultSize = reply.getTransactionsList().size();
                        totalCount = totalCount + resultSize;
                        String outputStatus = String.format("Got page %1$s with %2$s rows, appended to file", pageNum, resultSize);

                        //got a chunk, append it to the file!
                        List<WalletTransaction> transactions = reply.getTransactionsList();
                        appendRows(outputFile, transactions);

                        context.outputStatus(outputStatus);

                        if (reply.getTransactionsList().size() < pageSize) {
                            //Got everything
                            done = true;
                        } else {
                            //keep going
                            pageNum++;
                        }
                    }
                } catch (Exception ex2) {
                    result.fail();
                    result.addMessage("-1", "Error looping through results", ex2.getMessage(), true);
                    done = true;
                }
            } //end of loop

            //success
            FormattableObject<String> temp2 = new FormattableObject<>(new ArrayList<>());
            temp2.success = !result.didFail();
            temp2.payload = String.format("Wrote %1$s wallet transactions to file %2$s",
                    totalCount, outputFile);
            context.outputObject(temp2);

            context.suggestCommands(Arrays.asList(
                    GetBalanceCommand.class,
                    GetTransactionCommand.class
            ));

        } catch (StatusRuntimeException e) {
            CommandUtility.handleRuntimeException(result, e, _logger);
        }

        return result;
    }

    private GetWalletTransactionsReply getTransactions(CommandContext context, String address, int page, int itemsPerPage, WalletTransaction.Type transactionType) {

        GetWalletTransactionsRequest.Builder requestBuilder = GetWalletTransactionsRequest.newBuilder();

        requestBuilder.setAddress(ByteStringAddressUtility.createProperByteStringAutomatically(address));
        TransactionMeta.Status confirmed = TransactionMeta.Status.CONFIRMED;
        requestBuilder.setRequestType(GetWalletTransactionsRequest.Type.QUERY);
        requestBuilder.setStatus(confirmed);
        requestBuilder.setPage(Paging.newBuilder()
                .setPageNumber(page)
                .setResultsPerPage(itemsPerPage).build());
        requestBuilder.setTransactionType(transactionType);

        return context.adminService().getWalletTransactions(requestBuilder.build());
    }

    private void startFileHeader(String filename)
    {
        //Delete file if exists
        File file = new File(filename);
        if (file.exists() && file.isFile()) {
            file.delete();
        }

        //Append Header
        String s = String.format("%1$s,%2$s,%3$s,%4$s,%5$s,%6$s,%7$s,%8$s,%9$s,%10$s,%11$s",
                "block_height", "confirmations", "status",
                "transaction_type", "address_mine", "address_from", "address_to",
                "amount", "transaction_id", "timestamp",
                System.getProperty("line.separator"));

        appendFile(filename, s);
    }

    private void appendRows(String filename, List<WalletTransaction> transactions) {
        if (transactions == null) {
            return;
        }

        StringBuilder sb = new StringBuilder();
        for (WalletTransaction transaction : transactions) {
            WalletTransactionInfo row = new WalletTransactionInfo(transaction);
            String s = String.format("%1$s,%2$s,%3$s,%4$s,%5$s,%6$s,%7$s,%8$s,%9$s,%10$s,%11$s",
                    row.getBlockHeight(), row.getConfirmations(), row.getStatus(),
                    row.getTxType(), row.getAddressMine(), row.getAddressFrom(), row.getAddressTo(),
                    row.getAmount(), row.getTxId(), row.getTimestamp(),
                    System.getProperty("line.separator"));
            sb.append(s);
        }
        appendFile(filename, sb.toString());
    }

    //NOTE - could optimize this by keeping the file open. But keep it simple for now
    private static void appendFile(String filename, String line) {
        try {
            FileWriter fw = new FileWriter(filename, true);

            fw.write(line);

            fw.close();
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }
}
