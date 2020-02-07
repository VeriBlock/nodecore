package nodecore.cli.commands.rpc

import com.google.protobuf.ByteString
import io.grpc.StatusRuntimeException
import nodecore.api.grpc.VeriBlockMessages
import nodecore.api.grpc.utilities.ByteStringAddressUtility
import nodecore.cli.CliShell
import nodecore.cli.cliShell
import nodecore.cli.commands.ShellCommandParameterMappers
import nodecore.cli.prepareResult
import nodecore.cli.rpcCommand
import nodecore.cli.serialization.WalletTransactionInfo
import nodecore.cli.utilities.CommandUtility
import org.veriblock.core.utilities.createLogger
import org.veriblock.shell.CommandFactory
import org.veriblock.shell.CommandParameter
import org.veriblock.shell.CommandParameterMappers
import org.veriblock.shell.core.failure
import org.veriblock.shell.core.success
import java.io.File
import java.io.FileWriter

private val logger = createLogger {}

fun CommandFactory.walletCommands() {
    rpcCommand(
        name = "Backup Wallet",
        form = "backupwallet",
        description = "Backup the wallet of a NodeCore instance",
        parameters = listOf(
            CommandParameter(name = "targetLocation", mapper = CommandParameterMappers.STRING, required = true)
        ),
        suggestedCommands = { listOf("importwallet", "dumpprivatekey", "importprivatekey") }
    ) {
        val targetLocation: String = getParameter("targetLocation")
        val request = VeriBlockMessages.BackupWalletRequest.newBuilder()
            .setTargetLocation(ByteString.copyFrom(targetLocation.toByteArray()))
            .build()
        val result = cliShell.adminService.backupWallet(request)

        prepareResult(result.success, result.resultsList) {
            printInfo("Note: The backed-up wallet file is saved on the computer where NodeCore is running.")
            printInfo("Note: If the wallet is encrypted, the backup will require the password in use at the time the backup was created.")
        }
    }

    rpcCommand(
        name = "Decrypt Wallet",
        form = "decryptwallet",
        description = "Decrypts the wallet loaded in NodeCore",
        suggestedCommands = { listOf("encryptwallet") }
    ) {
        val passphrase = shell.passwordPrompt("Enter passphrase: ")
        val request = VeriBlockMessages.DecryptWalletRequest.newBuilder()
            .setPassphrase(passphrase)
            .build()
        val result = cliShell.adminService.decryptWallet(request)

        prepareResult(result.success, result.resultsList)
    }

    rpcCommand(
        name = "Encrypt Wallet",
        form = "encryptwallet",
        description = "Encrypts the wallet loaded in NodeCore with a passphrase",
        suggestedCommands = { listOf("decryptwallet", "unlockwallet", "lockwallet") }
    ) {
        val passphrase = shell.passwordPrompt("Enter passphrase: ")
        if (passphrase.isNullOrEmpty()) {
            failure("V060", "Invalid Passphrase", "Passphrase cannot be empty")
        } else {
            val confirmation = shell.passwordPrompt("Confirm passphrase: ")
            if (passphrase == confirmation) {
                val request = VeriBlockMessages.EncryptWalletRequest.newBuilder()
                    .setPassphrase(passphrase).build()
                val result = cliShell.adminService.encryptWallet(request)

                prepareResult(result.success, result.resultsList)
            } else {
                failure("V060", "Invalid Passphrase", "Passphrase and confirmation do not match")
            }
        }
    }

    rpcCommand(
        name = "Get wallet transactions",
        form = "getwallettransactions",
        description = "Writes transaction history for an address to a local file. This could take a while.",
        parameters = listOf(
            CommandParameter(name = "address", mapper = ShellCommandParameterMappers.STANDARD_ADDRESS, required = true),
            CommandParameter(name = "type", mapper = CommandParameterMappers.STRING, required = false)
        ),
        suggestedCommands = { listOf("getbalance", "gettransaction") }
    ) {
        val pageSize = 1000

        val address: String = getParameter("address")
        val type: String? = getOptionalParameter("type")
        val relativeFile = "$address.csv"

        var result = success()
        var totalCount = 0

        try {
            val outputFile = File(relativeFile).canonicalPath.toString()
            //Want to output this immediately so that user knows where the exact file is, and could monitor it
            printInfo(String.format("Append to file: %1\$s", outputFile))
            val transactionType: VeriBlockMessages.WalletTransaction.Type
            transactionType = type?.let { getTxType(it) } ?: VeriBlockMessages.WalletTransaction.Type.NOT_SET
            var pageNum = 1
            var done = false
            //Delete the file if it exists, in preparation for creating new file
            startFileHeader(outputFile)
            while (!done) { //get the data
                val reply = cliShell.getTransactions(address, pageNum, pageSize, transactionType)
                try {
                    if (reply == null) {
                        result = failure()
                        done = true
                    } else if (reply.cacheState != VeriBlockMessages.GetWalletTransactionsReply.CacheState.CURRENT) { //bad
                        result = failure("-2", "Address CacheState not CURRENT", reply.message)
                        done = true
                    } else {
                        val resultSize = reply.transactionsList.size
                        totalCount = totalCount + resultSize
                        val outputStatus = String.format("Got page %1\$s with %2\$s rows, appended to file", pageNum, resultSize)
                        //got a chunk, append it to the file!
                        val transactions = reply.transactionsList
                        appendRows(outputFile, transactions)
                        printInfo(outputStatus)
                        if (reply.transactionsList.size < pageSize) { //Got everything
                            done = true
                        } else { //keep going
                            pageNum++
                        }
                    }
                } catch (ex2: Exception) {
                    result = failure("-1", "Error looping through results", ex2.message ?: "")
                    done = true
                }
            } //end of loop
        } catch (e: StatusRuntimeException) {
            result = CommandUtility.handleRuntimeException(e, logger)
        }

        prepareResult(!result.isFailed, result.getMessages().map { VeriBlockMessages.Result.newBuilder().build() }) {
            String.format("Wrote $totalCount wallet transactions to file $outputFile",
                totalCount, outputFile)
        }
    }

    rpcCommand(
        name = "Import Wallet",
        form = "importwallet",
        description = "Import a NodeCore wallet backup",
        parameters = listOf(
            CommandParameter(name = "sourceLocation", mapper = CommandParameterMappers.STRING, required = true)
        ),
        suggestedCommands = { listOf("dumpprivatekey", "importprivatekey", "backupwallet") }
    ) {
        val sourceLocation: String = getParameter("sourceLocation")
        val passphrase = shell.passwordPrompt("Enter passphrase of importing wallet (Press ENTER if not password-protected): ")
        val request = VeriBlockMessages.ImportWalletRequest.newBuilder()
            .setSourceLocation(ByteString.copyFrom(sourceLocation.toByteArray()))

        if (!passphrase.isNullOrEmpty()) {
            request.passphrase = passphrase
        }

        val result = cliShell.adminService.importWallet(request.build())

        prepareResult(result.success, result.resultsList)
    }

    rpcCommand(
        name = "Lock Wallet",
        form = "lockwallet",
        description = "Disables the temporary unlock on the NodeCore wallet",
        suggestedCommands = { listOf("unlockwallet") }
    ) {
        val request = VeriBlockMessages.LockWalletRequest.newBuilder().build()
        val result = cliShell.adminService.lockWallet(request)

        prepareResult(result.success, result.resultsList)
    }

    rpcCommand(
        name = "Unlock Wallet",
        form = "unlockwallet",
        description = "Temporarily unlocks the NodeCore wallet",
        suggestedCommands = { listOf("lockwallet") }
    ) {
        val passphrase = shell.passwordPrompt("Enter passphrase: ")
        val request = VeriBlockMessages.UnlockWalletRequest.newBuilder()
            .setPassphrase(passphrase).build()
        val result = cliShell.adminService.unlockWallet(request)

        prepareResult(result.success, result.resultsList)
    }

    rpcCommand(
        name = "Refresh Wallet Cache",
        form = "refreshwalletcache",
        description = "Rescans the blockchain for transactions belonging to imported wallets"
    ) {
        val request = VeriBlockMessages.RefreshWalletCacheRequest.newBuilder().build()
        val result = cliShell.adminService.refreshWalletCache(request)

        prepareResult(result.success, result.resultsList)
    }
}

private fun getTxType(type: String) = when (type) {
    "popcoinbase" -> VeriBlockMessages.WalletTransaction.Type.POP_COINBASE
    "powcoinbase" -> VeriBlockMessages.WalletTransaction.Type.POW_COINBASE
    "coinbase" -> VeriBlockMessages.WalletTransaction.Type.BOTH_COINBASE
    "pop" -> VeriBlockMessages.WalletTransaction.Type.POP
    "received" -> VeriBlockMessages.WalletTransaction.Type.RECEIVED
    "sent" -> VeriBlockMessages.WalletTransaction.Type.SENT
    else -> VeriBlockMessages.WalletTransaction.Type.NOT_SET
}


private fun CliShell.getTransactions(
    address: String,
    page: Int,
    itemsPerPage: Int,
    transactionType: VeriBlockMessages.WalletTransaction.Type
): VeriBlockMessages.GetWalletTransactionsReply? {
    val requestBuilder = VeriBlockMessages.GetWalletTransactionsRequest.newBuilder()
    requestBuilder.address = ByteStringAddressUtility.createProperByteStringAutomatically(address)
    requestBuilder.requestType = VeriBlockMessages.GetWalletTransactionsRequest.Type.QUERY
    requestBuilder.status = VeriBlockMessages.TransactionMeta.Status.CONFIRMED
    requestBuilder.page = VeriBlockMessages.Paging.newBuilder()
        .setPageNumber(page)
        .setResultsPerPage(itemsPerPage).build()
    requestBuilder.transactionType = transactionType
    return adminService.getWalletTransactions(requestBuilder.build())
}

private fun startFileHeader(filename: String) { //Delete file if exists
    val file = File(filename)
    if (file.exists() && file.isFile) {
        file.delete()
    }
    //Append Header
    val s = String.format("%1\$s,%2\$s,%3\$s,%4\$s,%5\$s,%6\$s,%7\$s,%8\$s,%9\$s,%10\$s,%11\$s",
        "block_height", "confirmations", "status",
        "transaction_type", "address_mine", "address_from", "address_to",
        "amount", "transaction_id", "timestamp",
        System.getProperty("line.separator"))
    appendFile(filename, s)
}

private fun appendRows(filename: String, transactions: List<VeriBlockMessages.WalletTransaction>?) {
    if (transactions == null) {
        return
    }
    val sb = StringBuilder()
    for (transaction in transactions) {
        val row = WalletTransactionInfo(transaction)
        val s = String.format("%1\$s,%2\$s,%3\$s,%4\$s,%5\$s,%6\$s,%7\$s,%8\$s,%9\$s,%10\$s,%11\$s",
            row.blockHeight, row.confirmations, row.status,
            row.txType, row.addressMine, row.addressFrom, row.addressTo,
            row.amount, row.txId, row.timestamp,
            System.getProperty("line.separator"))
        sb.append(s)
    }
    appendFile(filename, sb.toString())
}

/**
 * NOTE - could optimize this by keeping the file open. But keep it simple for now
 */
private fun appendFile(filename: String, line: String) {
    try {
        FileWriter(filename, true).use { fw -> fw.write(line) }
    } catch (ex: Exception) {
        ex.printStackTrace()
    }
}
