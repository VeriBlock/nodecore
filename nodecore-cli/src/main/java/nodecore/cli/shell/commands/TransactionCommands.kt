package nodecore.cli.shell.commands

import org.veriblock.shell.Shell
import org.veriblock.shell.command

fun Shell.transactionCommands() {
    command(
        name = "Abandon Transaction From TxID",
        form = "abandontransactionfromtxid",
        description = "Abandons the specified pending transaction and all dependent transactions"
    ) {

    }

    command(
        name = "Abandon Transactions From Address",
        form = "abandontransactionsfromaddress",
        description = "Abandons all pending transactions from a particular source address (optionally above a particular signature index), and all dependent transactions"
    ) {

    }

    command(
        name = "Get Pending Transactions",
        form = "getpendingtransactions",
        description = "Returns the transactions pending on the network"
    ) {

    }

    command(
        name = "Get Transaction",
        form = "gettransaction",
        description = "Gets information regarding provided TxID"
    ) {

    }
}
