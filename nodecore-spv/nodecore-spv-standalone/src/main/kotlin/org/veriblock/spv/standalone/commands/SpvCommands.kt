package org.veriblock.spv.standalone.commands

import com.google.gson.GsonBuilder
import com.google.gson.JsonPrimitive
import com.google.gson.JsonSerializer
import kotlinx.coroutines.runBlocking
import org.jline.utils.AttributedStyle
import org.veriblock.core.WalletException
import org.veriblock.core.utilities.Utility
import org.veriblock.sdk.models.Coin
import org.veriblock.sdk.models.asCoin
import org.veriblock.shell.CommandContext
import org.veriblock.shell.CommandFactory
import org.veriblock.shell.CommandParameter
import org.veriblock.shell.CommandParameterMappers
import org.veriblock.shell.command
import org.veriblock.shell.core.failure
import org.veriblock.shell.core.success
import org.veriblock.spv.SpvContext
import org.veriblock.spv.model.Output
import org.veriblock.spv.model.asLightAddress

fun CommandFactory.spvCommands(
    context: SpvContext
) {
    command(
        name = "Get Balance",
        form = "getbalance|getbal|bal",
        description = "Displays the balances of all of your addresses",
        parameters = listOf(
            CommandParameter(name = "address", mapper = ShellCommandParameterMappers.STANDARD_ADDRESS, required = false)
        ),
        suggestedCommands = { listOf("getnewaddress") }
    ) {
        val result = context.spvService.getBalance()
        displayResult(result)
        success()
    }

    command(
        name = "Get state Info",
        form = "getstateinfo|stateinfo|state|getstate",
        description = "Displays blockchain, operating, and network state information",
        suggestedCommands = { listOf("getbalance") }
    ) {
        val result = context.spvService.getStateInfo()
        displayResult(result)
        success()
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

        val result = runBlocking {
            context.spvService.sendCoins(
                sourceAddress = sourceAddress?.asLightAddress(),
                outputs = listOf(
                    Output(
                        destinationAddress.asLightAddress(),
                        atomicAmount.asCoin()
                    )
                )
            )
        }

        printInfo("Transaction ids:")
        displayResult(result.map { it.toString() })
        success()
    }

    command(
        name = "Lock Wallet",
        form = "lockwallet",
        description = "Locks the loaded SPV wallet, this command has no effect on the wallet file",
        suggestedCommands = { listOf("unlockwallet") }
    ) {
        context.spvService.lockWallet()
        success()
    }

    command(
        name = "Unlock Wallet",
        form = "unlockwallet",
        description = "Unlocks the loaded SPV wallet, this command has no effect on the wallet file",
        suggestedCommands = { listOf("lockwallet") }
    ) {
        val passphrase = shell.passwordPrompt("Enter passphrase: ")
        context.spvService.unlockWallet(passphrase)
        success()
    }

    command(
        name = "Decrypt Wallet",
        form = "decryptwallet",
        description = "Decrypts the SPV wallet file with the given passphrase",
        suggestedCommands = { listOf("encryptwallet") }
    ) {
        val passphrase = shell.passwordPrompt("Enter passphrase: ")
        context.spvService.decryptWallet(passphrase)
        printInfo("Wallet has been decrypted")
        success()
    }

    command(
        name = "Encrypt Wallet",
        form = "encryptwallet",
        description = "Encrypts the SPV wallet file with the given passphrase",
        suggestedCommands = { listOf("decryptwallet", "unlockwallet", "lockwallet") }
    ) {
        val passphrase = shell.passwordPrompt("Enter passphrase: ")
        if (passphrase.isEmpty()) {
            failure("V060", "Invalid Passphrase", "Passphrase cannot be empty")
        } else {
            val confirmation = shell.passwordPrompt("Confirm passphrase: ")
            if (passphrase == confirmation) {
                context.spvService.encryptWallet(passphrase)
                printInfo("Wallet has been encrypted with supplied passphrase")
                success()
            } else {
                throw WalletException("Passphrase and confirmation do not match")
            }
        }
    }

    command(
        name = "Import Wallet",
        form = "importwallet",
        description = "Imports a SPV wallet file",
        parameters = listOf(
            CommandParameter(name = "sourceLocation", mapper = CommandParameterMappers.STRING, required = true)
        ),
        suggestedCommands = { listOf("backupwallet") }
    ) {
        val sourceLocation: String = getParameter("sourceLocation")
        val passphrase = shell.passwordPrompt("Enter passphrase of importing wallet (Press ENTER if not password-protected): ")
        context.spvService.importWallet(sourceLocation, passphrase)
        success()
    }

    command(
        name = "Backup Wallet",
        form = "backupwallet",
        description = "Creates a backup from the loaded SPV wallet",
        parameters = listOf(
            CommandParameter(name = "targetLocation", mapper = CommandParameterMappers.STRING, required = true)
        ),
        suggestedCommands = { listOf("importwallet") }
    ) {
        val targetLocation: String = getParameter("targetLocation")
        context.spvService.backupWallet(targetLocation)
        printInfo("Note: The backed-up wallet file is saved on the computer where SPV is running.")
        printInfo("Note: If the wallet is encrypted, the backup will require the password in use at the time the backup was created.")
        success()
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
        val count = getOptionalParameter<Int>("count")?.coerceAtLeast(1) ?: 1

        val result = context.spvService.getNewAddress(count)
        printInfo("The wallet has been modified. Please make a backup of the wallet data file.")
        displayResult(result.map { it.hash })
        success()
    }
}

private val prettyPrintGson = GsonBuilder().apply {
    setPrettyPrinting()
    registerTypeAdapter(Coin::class.java, JsonSerializer<Coin> { src, _, _ ->
        JsonPrimitive(src.toString())
    })
}.create()

fun CommandContext.displayResult(
    result: Any
) {
    shell.printStyled(
        prettyPrintGson.toJson(result) + "\n",
        AttributedStyle.BOLD.foreground(AttributedStyle.GREEN)
    )
}
