package org.veriblock.spv.standalone.commands

import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.google.protobuf.ByteString
import com.google.protobuf.Message
import com.google.protobuf.util.JsonFormat
import nodecore.api.grpc.VeriBlockMessages
import nodecore.api.grpc.utilities.ByteStringAddressUtility
import org.jline.utils.AttributedStyle
import org.veriblock.core.InvalidAddressException
import org.veriblock.core.utilities.AddressUtility
import org.veriblock.core.utilities.Utility
import org.veriblock.sdk.models.Coin
import org.veriblock.shell.CommandContext
import org.veriblock.shell.CommandFactory
import org.veriblock.shell.CommandParameter
import org.veriblock.shell.CommandParameterMappers
import org.veriblock.shell.command
import org.veriblock.shell.core.Result
import org.veriblock.shell.core.failure
import veriblock.SpvContext
import veriblock.model.Output
import veriblock.model.asLightAddress

fun CommandFactory.spvCommands(
    context: SpvContext
) {
    command(
        name = "Get Balance",
        form = "getbalance|getbal|bal",
        description = "See the balances of all of your addresses",
        parameters = listOf(
            CommandParameter(name = "address", mapper = ShellCommandParameterMappers.STANDARD_ADDRESS, required = false)
        ),
        suggestedCommands = { listOf("getnewaddress") }
    ) {
        val address: String? = getOptionalParameter("address")
        val request = VeriBlockMessages.GetBalanceRequest.newBuilder()
        if (address != null) {
            request.addAddresses(ByteStringAddressUtility.createProperByteStringAutomatically(address));
        }
        val result = context.adminApiService.getBalance(request.build())

        prepareResult(result.success, result.resultsList, result)
    }

    command(
        name = "Get state Info",
        form = "getstateinfo|stateinfo|state|getstate",
        description = "Returns blockchain, operating, and network state information",
        suggestedCommands = { listOf("getbalance") }
    ) {
        val result = context.adminApiService.getStateInfo()
        displayResult(result)
    }

    command(
        name = "Send",
        form = "send|sendtoaddress|sendtoaddr",
        description = "Send coins to the specified address",
        parameters = listOf(
            CommandParameter(name = "amount", mapper = CommandParameterMappers.STRING, required = true),
            CommandParameter(name = "destinationAddress", mapper = ShellCommandParameterMappers.STANDARD_OR_MULTISIG_ADDRESS, required = true),
            CommandParameter(name = "sourceAddress", mapper = ShellCommandParameterMappers.STANDARD_ADDRESS, required = false)
        ),
        suggestedCommands = { listOf("getbalance") }
    ) {
        val amount: String = getParameter("amount")
        val atomicAmount = Utility.convertDecimalCoinToAtomicLong(amount)
        val destinationAddress: String = getParameter("destinationAddress")
        val sourceAddress: String? = getOptionalParameter("sourceAddress")

        val result = context.adminApiService.sendCoins(
            sourceAddress = sourceAddress?.asLightAddress(),
            outputs = listOf(
                Output(
                    destinationAddress.asLightAddress(),
                    Coin.valueOf(atomicAmount)
                )
            )
        )

        displayResult(result)
    }

    command(
        name = "Lock Wallet",
        form = "lockwallet",
        description = "Disables the temporary unlock on the NodeCore wallet",
        suggestedCommands = { listOf("unlockwallet") }
    ) {
        val request = VeriBlockMessages.LockWalletRequest.newBuilder().build()
        val result = context.adminApiService.lockWallet(request)

        prepareResult(result.success, result.resultsList, result)
    }

    command(
        name = "Unlock Wallet",
        form = "unlockwallet",
        description = "Temporarily unlocks the NodeCore wallet",
        suggestedCommands = { listOf("lockwallet") }
    ) {
        val passphrase = shell.passwordPrompt("Enter passphrase: ")
        val request = VeriBlockMessages.UnlockWalletRequest.newBuilder()
            .setPassphrase(passphrase).build()
        val result = context.adminApiService.unlockWallet(request)

        prepareResult(result.success, result.resultsList, result)
    }

    command(
        name = "Decrypt Wallet",
        form = "decryptwallet",
        description = "Decrypts the wallet loaded in NodeCore",
        suggestedCommands = { listOf("encryptwallet") }
    ) {
        val passphrase = shell.passwordPrompt("Enter passphrase: ")
        val request = VeriBlockMessages.DecryptWalletRequest.newBuilder()
            .setPassphrase(passphrase)
            .build()
        val result = context.adminApiService.decryptWallet(request)

        prepareResult(result.success, result.resultsList, result)
    }

    command(
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
                val result = context.adminApiService.encryptWallet(request)

                prepareResult(result.success, result.resultsList, result)
            } else {
                failure("V060", "Invalid Passphrase", "Passphrase and confirmation do not match")
            }
        }
    }

    command(
        name = "Import Wallet",
        form = "importwallet",
        description = "Import a NodeCore wallet backup",
        parameters = listOf(
            CommandParameter(name = "sourceLocation", mapper = CommandParameterMappers.STRING, required = true)
        ),
        suggestedCommands = { listOf("importprivatekey", "backupwallet") }
    ) {
        val sourceLocation: String = getParameter("sourceLocation")
        val passphrase = shell.passwordPrompt("Enter passphrase of importing wallet (Press ENTER if not password-protected): ")
        val request = VeriBlockMessages.ImportWalletRequest.newBuilder()
            .setSourceLocation(ByteString.copyFrom(sourceLocation.toByteArray()))

        if (!passphrase.isNullOrEmpty()) {
            request.passphrase = passphrase
        }

        val result = context.adminApiService.importWallet(request.build())

        prepareResult(result.success, result.resultsList, result)
    }

    command(
        name = "Backup Wallet",
        form = "backupwallet",
        description = "Backup the wallet of a NodeCore instance",
        parameters = listOf(
            CommandParameter(name = "targetLocation", mapper = CommandParameterMappers.STRING, required = true)
        ),
        suggestedCommands = { listOf("importwallet", "importprivatekey") }
    ) {
        val targetLocation: String = getParameter("targetLocation")
        val request = VeriBlockMessages.BackupWalletRequest.newBuilder()
            .setTargetLocation(ByteString.copyFrom(targetLocation.toByteArray()))
            .build()
        val result = context.adminApiService.backupWallet(request)

        printInfo("Note: The backed-up wallet file is saved on the computer where NodeCore is running.")
        printInfo("Note: If the wallet is encrypted, the backup will require the password in use at the time the backup was created.")
        prepareResult(result.success, result.resultsList, result)
    }

    command(
        name = "Get New Address",
        form = "getnewaddress",
        description = "Gets {count} new address from the wallet (default: 1)",
        parameters = listOf(
            CommandParameter(name = "count", mapper = CommandParameterMappers.INTEGER, required = false)
        ),
        suggestedCommands = { listOf("backupwallet", "getbalance") }
    ) {
        val count = (getOptionalParameter("count") ?: 1).coerceAtLeast(1)

        val result = context.adminApiService.getNewAddress(VeriBlockMessages.GetNewAddressRequest.newBuilder()
            .setCount(count)
            .build()
        )
        prepareResult(result.success, result.resultsList, result)
    }

    command(
        name = "Get New Address",
        form = "getnewaddress",
        description = "Gets {count} new address from the wallet (default: 1)",
        parameters = listOf(
            CommandParameter(name = "count", mapper = CommandParameterMappers.INTEGER, required = false)
        ),
        suggestedCommands = { listOf("backupwallet", "getbalance") }
    ) {
        val count = (getOptionalParameter("count") ?: 1).coerceAtLeast(1)

        val result = context.adminApiService.getNewAddress(VeriBlockMessages.GetNewAddressRequest.newBuilder()
            .setCount(count)
            .build()
        )
        prepareResult(result.success, result.resultsList, result)
    }
}

fun CommandContext.prepareResult(
    success: Boolean,
    results: List<VeriBlockMessages.Result>,
    payload: Message
): Result {
    return if (!success) {
        failure(results)
    } else {
        shell.printStyled(
            JsonFormat.printer().print(payload) + "\n",
            AttributedStyle.BOLD.foreground(AttributedStyle.GREEN)
        )
        success(results)
    }
}

fun CommandContext.displayResult(
    result: Any
): Result {
    shell.printStyled(
        Gson().toJson(result) + "\n",
        AttributedStyle.BOLD.foreground(AttributedStyle.GREEN)
    )
    return org.veriblock.shell.core.success()
}

fun failure(results: List<VeriBlockMessages.Result>) = failure {
    for (r in results) {
        addMessage(r.code, r.message, r.details, r.error)
    }
}

fun success(results: List<VeriBlockMessages.Result>) = org.veriblock.shell.core.success {
    for (r in results) {
        addMessage(r.code, r.message, r.details, r.error)
    }
}
