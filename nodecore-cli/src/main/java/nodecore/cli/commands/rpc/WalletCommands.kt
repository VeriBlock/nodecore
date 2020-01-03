package nodecore.cli.commands.rpc

import com.google.protobuf.ByteString
import nodecore.api.grpc.VeriBlockMessages
import nodecore.cli.CliShell
import nodecore.cli.contracts.AdminService
import nodecore.cli.prepareResult
import org.veriblock.shell.CommandParameter
import org.veriblock.shell.CommandParameterType
import org.veriblock.shell.Shell
import org.veriblock.shell.command
import org.veriblock.shell.core.failure

fun CliShell.walletCommands() {
    rpcCommand(
        name = "Backup Wallet",
        form = "backupwallet",
        description = "Backup the wallet of a NodeCore instance",
        parameters = listOf(
            CommandParameter(name = "targetLocation", type = CommandParameterType.STRING, required = true)
        ),
        suggestedCommands = listOf("importwallet", "dumpprivatekey", "importprivatekey")
    ) {
        val targetLocation: String = getParameter("targetLocation")
        val request = VeriBlockMessages.BackupWalletRequest.newBuilder()
            .setTargetLocation(ByteString.copyFrom(targetLocation.toByteArray()))
            .build()
        val result = adminService.backupWallet(request)

        prepareResult(result.success, result.resultsList) {
            printInfo("Note: The backed-up wallet file is saved on the computer where NodeCore is running.")
            printInfo("Note: If the wallet is encrypted, the backup will require the password in use at the time the backup was created.")
        }
    }

    rpcCommand(
        name = "Decrypt Wallet",
        form = "decryptwallet",
        description = "Decrypts the wallet loaded in NodeCore",
        suggestedCommands = listOf("encryptwallet")
    ) {
        val passphrase = passwordPrompt("Enter passphrase: ")
        val request = VeriBlockMessages.DecryptWalletRequest.newBuilder()
            .setPassphrase(passphrase)
            .build()
        val result = adminService.decryptWallet(request)

        prepareResult(result.success, result.resultsList)
    }

    rpcCommand(
        name = "Encrypt Wallet",
        form = "encryptwallet",
        description = "Encrypts the wallet loaded in NodeCore with a passphrase",
        suggestedCommands = listOf("decryptwallet", "unlockwallet", "lockwallet")
    ) {
        val passphrase = passwordPrompt("Enter passphrase: ")
        if (passphrase.isNullOrEmpty()) {
            failure("V060", "Invalid Passphrase", "Passphrase cannot be empty")
        } else {
            val confirmation = passwordPrompt("Confirm passphrase: ")
            if (passphrase == confirmation) {
                val request = VeriBlockMessages.EncryptWalletRequest.newBuilder()
                    .setPassphrase(passphrase).build()
                val result = adminService.encryptWallet(request)

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
            CommandParameter(name = "address", type = CommandParameterType.STANDARD_ADDRESS, required = true),
            CommandParameter(name = "type", type = CommandParameterType.STRING, required = false)
        ),
        suggestedCommands = listOf("")
    ) {
        // TODO
    }

    rpcCommand(
        name = "Import Wallet",
        form = "importwallet",
        description = "Import a NodeCore wallet backup",
        parameters = listOf(
            CommandParameter(name = "sourceLocation", type = CommandParameterType.STRING, required = true)
        ),
        suggestedCommands = listOf("dumpprivatekey", "importprivatekey", "backupwallet")
    ) {
        val sourceLocation: String = getParameter("sourceLocation")
        val passphrase = passwordPrompt("Enter passphrase of importing wallet (Press ENTER if not password-protected): ")
        val request = VeriBlockMessages.ImportWalletRequest.newBuilder()
            .setSourceLocation(ByteString.copyFrom(sourceLocation.toByteArray()))

        if (!passphrase.isNullOrEmpty()) {
            request.passphrase = passphrase
        }

        val result = adminService.importWallet(request.build())

        prepareResult(result.success, result.resultsList)
    }

    rpcCommand(
        name = "Lock Wallet",
        form = "lockwallet",
        description = "Disables the temporary unlock on the NodeCore wallet",
        suggestedCommands = listOf("unlockwallet")
    ) {
        val request = VeriBlockMessages.LockWalletRequest.newBuilder().build()
        val result = adminService.lockWallet(request)

        prepareResult(result.success, result.resultsList)
    }

    rpcCommand(
        name = "Unlock Wallet",
        form = "unlockwallet",
        description = "Temporarily unlocks the NodeCore wallet",
        suggestedCommands = listOf("lockwallet")
    ) {
        val passphrase = passwordPrompt("Enter passphrase: ")
        val request = VeriBlockMessages.UnlockWalletRequest.newBuilder()
            .setPassphrase(passphrase).build()
        val result = adminService.unlockWallet(request)

        prepareResult(result.success, result.resultsList)
    }

    rpcCommand(
        name = "Refresh Wallet Cache",
        form = "refreshwalletcache",
        description = "Rescans the blockchain for transactions belonging to imported wallets"
    ) {
        val request = VeriBlockMessages.RefreshWalletCacheRequest.newBuilder().build()
        val result = adminService.refreshWalletCache(request)

        prepareResult(result.success, result.resultsList)
    }
}
