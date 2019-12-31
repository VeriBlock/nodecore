package nodecore.cli.shell.commands

import org.veriblock.shell.Shell
import org.veriblock.shell.command

fun Shell.walletCommands() {
    command(
        name = "Backup Wallet",
        form = "backupwallet",
        description = "Backup the wallet of a NodeCore instance"
    ) {

    }

    command(
        name = "Decrypt Wallet",
        form = "decryptwallet",
        description = "Decrypts the wallet loaded in NodeCore"
    ) {

    }

    command(
        name = "Encrypt Wallet",
        form = "encryptwallet",
        description = "Encrypts the wallet loaded in NodeCore with a passphrase"
    ) {

    }

    command(
        name = "Get wallet transactions",
        form = "getwallettransactions",
        description = "Writes transaction history for an address to a local file. This could take a while."
    ) {

    }

    command(
        name = "Import Wallet",
        form = "importwallet",
        description = "Import a NodeCore wallet backup"
    ) {

    }

    command(
        name = "Lock Wallet",
        form = "lockwallet",
        description = "Disables the temporary unlock on the NodeCore wallet"
    ) {

    }

    command(
        name = "Unlock Wallet",
        form = "unlockwallet",
        description = "Temporarily unlocks the NodeCore wallet"
    ) {

    }

    command(
        name = "Refresh Wallet Cache",
        form = "refreshwalletcache",
        description = "Rescans the blockchain for transactions belonging to imported wallets"
    ) {

    }
}
