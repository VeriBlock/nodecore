package nodecore.cli.commands.rpc

import com.google.protobuf.ByteString
import nodecore.api.grpc.RpcAbandonAddressTransactionsRequest
import nodecore.api.grpc.RpcAbandonTransactionRequest
import nodecore.api.grpc.RpcAddressIndexPair
import nodecore.api.grpc.RpcGetLastBitcoinFinalizedBlockRequest
import nodecore.api.grpc.RpcGetPendingTransactionsRequest
import nodecore.api.grpc.RpcGetTransactionsRequest
import nodecore.api.grpc.RpcMakeUnsignedMultisigTxRequest
import nodecore.api.grpc.RpcMultisigBundle
import nodecore.api.grpc.RpcMultisigSlot
import nodecore.api.grpc.RpcOutput
import nodecore.api.grpc.RpcRebroadcastAddressTransactionsRequest
import nodecore.api.grpc.RpcRebroadcastTransactionRequest
import nodecore.api.grpc.RpcSendCoinsRequest
import nodecore.api.grpc.RpcSetTransactionFeeRequest
import nodecore.api.grpc.RpcSignedMultisigTransaction
import nodecore.api.grpc.RpcSubmitMultisigTxRequest
import nodecore.api.grpc.RpcSubmitTransactionsRequest
import nodecore.api.grpc.RpcTransaction
import nodecore.api.grpc.RpcTransactionSet
import nodecore.api.grpc.RpcTransactionUnion
import nodecore.api.grpc.RpcUnsignedMultisigTransactionWithIndex
import org.veriblock.sdk.extensions.ByteStringAddressUtility
import org.veriblock.sdk.extensions.ByteStringUtility
import org.veriblock.sdk.extensions.asHexByteString
import nodecore.cli.cliShell
import nodecore.cli.commands.ShellCommandParameterMappers
import nodecore.cli.prepareResult
import nodecore.cli.rpcCommand
import nodecore.cli.serialization.AbandonTransactionFromTxIDInfo
import nodecore.cli.serialization.AbandonTransactionsFromAddressInfo
import nodecore.cli.serialization.MakeUnsignedMultisigTxPayload
import nodecore.cli.serialization.RebroadcastTransactionFromTxIDInfo
import nodecore.cli.serialization.RebroadcastTransactionsFromAddressInfo
import nodecore.cli.serialization.SendCoinsPayload
import nodecore.cli.serialization.TransactionInfo
import nodecore.cli.serialization.TransactionReferencesPayload
import org.veriblock.core.utilities.AddressUtility
import org.veriblock.core.utilities.Utility
import org.veriblock.core.utilities.extensions.asHexBytes
import org.veriblock.shell.CommandFactory
import org.veriblock.shell.CommandParameter
import org.veriblock.shell.CommandParameterMappers
import org.veriblock.shell.core.failure

fun CommandFactory.transactionCommands() {
    rpcCommand(
        name = "Abandon Transaction From TxID",
        form = "abandontransactionfromtxid",
        description = "Abandons the specified pending transaction and all dependent transactions",
        parameters = listOf(
            CommandParameter(name = "txId", mapper = CommandParameterMappers.STRING, required = true)
        )
    ) {
        val txid: String = getParameter("txId")
        val request = RpcAbandonTransactionRequest.newBuilder()
            .setTxids(
                RpcTransactionSet.newBuilder()
                    .addTxids(txid.asHexByteString())
            ).build()
        val result = cliShell.adminService.abandonTransactionRequest(request)

        prepareResult(result.success, result.resultsList) {
            AbandonTransactionFromTxIDInfo(result)
        }
    }

    rpcCommand(
            name = "Rebroadcast Transaction From TxID",
            form = "rebroadcasttransactionfromtxid",
            description = "Rebroadcasts the specified pending transaction and all dependent transactions",
            parameters = listOf(
                    CommandParameter(name = "txId", mapper = CommandParameterMappers.STRING, required = true)
            )
    ) {
        val txid: String = getParameter("txId")
        val request = RpcRebroadcastTransactionRequest.newBuilder()
                .setTxids(RpcTransactionSet.newBuilder()
                        .addTxids(ByteString.copyFrom(Utility.hexToBytes(txid)))
                ).build()
        val result = cliShell.adminService.rebroadcastTransactionRequest(request)

        prepareResult(result.success, result.resultsList) {
            RebroadcastTransactionFromTxIDInfo(result)
        }
    }

    rpcCommand(
        name = "Abandon Transactions From Address",
        form = "abandontransactionsfromaddress",
        description = "Abandons all pending transactions from a particular source address (optionally above a particular signature index), and all dependent transactions",
        parameters = listOf(
            CommandParameter(name = "address", mapper = CommandParameterMappers.STRING, required = true),
            CommandParameter(name = "index", mapper = CommandParameterMappers.LONG, required = false)
        )
    ) {
        val address: String = getParameter("address")
        val index: Long = getParameter("index") ?: 0
        val request = RpcAbandonTransactionRequest.newBuilder()
            .setAddresses(
                RpcAbandonAddressTransactionsRequest.newBuilder()
                    .addAddresses(
                        RpcAddressIndexPair.newBuilder()
                            .setAddress(ByteStringAddressUtility.createProperByteStringAutomatically(address))
                            .setStartingSignatureIndex(index)))
            .build()

        val result = cliShell.adminService.abandonTransactionRequest(request)

        prepareResult(result.success, result.resultsList) {
            AbandonTransactionsFromAddressInfo(result)
        }
    }

    rpcCommand(
            name = "Rebroadcast Transactions From Address",
            form = "rebroadcasttransactionsfromaddress",
            description = "Rebroadcasts all pending transactions from a particular source address (optionally above a particular signature index), and all dependent transactions",
            parameters = listOf(
                    CommandParameter(name = "address", mapper = CommandParameterMappers.STRING, required = true),
                    CommandParameter(name = "index", mapper = CommandParameterMappers.LONG, required = false)
            )
    ) {
        val address: String = getParameter("address")
        val index: Long = getParameter("index") ?: 0
        val request = RpcRebroadcastTransactionRequest.newBuilder()
            .setAddresses(
                RpcRebroadcastAddressTransactionsRequest.newBuilder()
                    .addAddresses(
                        RpcAddressIndexPair.newBuilder()
                            .setAddress(ByteStringAddressUtility.createProperByteStringAutomatically(address))
                            .setStartingSignatureIndex(index)
                    )
            )
            .build()

        val result = cliShell.adminService.rebroadcastTransactionRequest(request)

        prepareResult(result.success, result.resultsList) {
            RebroadcastTransactionsFromAddressInfo(result)
        }
    }

    rpcCommand(
        name = "Get Pending Transactions",
        form = "getpendingtransactions",
        description = "Returns the transactions pending on the network",
        suggestedCommands = { listOf("send", "getbalance", "gethistory", "sigindex") }
    ) {
        val request = RpcGetPendingTransactionsRequest.newBuilder().build()
        val result = cliShell.adminService.getPendingTransactions(request)

        prepareResult(result.success, result.resultsList) {
            result.transactionsList.map { TransactionInfo(it) }
        }
    }

    rpcCommand(
        name = "Get Transaction",
        form = "gettransaction",
        description = "Gets information regarding provided TxID",
        parameters = listOf(
            CommandParameter(name = "txId", mapper = CommandParameterMappers.STRING, required = true),
            CommandParameter(name = "searchLength", mapper = CommandParameterMappers.INTEGER, required = false)
        ),
        suggestedCommands = { listOf("getbalance", "gethistory") }
    ) {
        val transactionId: String = getParameter("txId")
        val searchLength: Int? = getOptionalParameter("searchLength")
        val request = RpcGetTransactionsRequest.newBuilder()
            .addIds(transactionId.asHexByteString())

        if (searchLength != null) {
            request.searchLength = searchLength
        }

        val result = cliShell.adminService.getTransactions(request.build())

        prepareResult(result.success, result.resultsList) {
            TransactionReferencesPayload(result.transactionsList)
        }
    }

    rpcCommand(
        name = "Get last BTC finalized block",
        form = "getlastbtcfinalizedblock",
        description = "Gets information regarding last BTC finalized block by bitcoint confirmation number",
        parameters = listOf(
            CommandParameter(name = "bitcoinConfirmations", mapper = CommandParameterMappers.INTEGER, required = true)
        )
    ) {
        val bitcoinConfirmations: Int = getParameter("bitcoinConfirmations")
        val request = RpcGetLastBitcoinFinalizedBlockRequest.newBuilder().setBitcoinConfirmations(bitcoinConfirmations)

        val result = cliShell.adminService.getLastBitcoinFinalizedBlock(request.build())

        prepareResult(result.success, result.resultsList) {
           result.lastBtcFinalizedBlock
        }
    }

    rpcCommand(
        name = "Send",
        form = "send|sendtoaddress|sendtoaddr",
        description = "Send coins to the specified address",
        parameters = listOf(
            CommandParameter(name = "amount", mapper = CommandParameterMappers.STRING, required = true),
            CommandParameter(name = "destinationAddress", mapper = ShellCommandParameterMappers.STANDARD_OR_MULTISIG_ADDRESS, required = true),
            CommandParameter(name = "sourceAddress", mapper = ShellCommandParameterMappers.STANDARD_ADDRESS, required = false),
            CommandParameter(name = "takeFeeFromOutputs", mapper = CommandParameterMappers.BOOLEAN, required = false)
        ),
        suggestedCommands = { listOf("gethistory", "getbalance") }
    ) {
        val amount: String = getParameter("amount")
        val atomicAmount = Utility.convertDecimalCoinToAtomicLong(amount)
        val destinationAddress: String = getParameter("destinationAddress")
        val sourceAddress: String? = getOptionalParameter("sourceAddress")
        val takeFeeFromOutputs: Boolean? = getOptionalParameter("takeFeeFromOutputs")

        val request = RpcSendCoinsRequest.newBuilder()

        if (AddressUtility.isValidStandardOrMultisigAddress(destinationAddress)) {
            request.addAmounts(
                RpcOutput.newBuilder()
                    .setAddress(ByteStringAddressUtility.createProperByteStringAutomatically(destinationAddress))
                    .setAmount(atomicAmount)
            )

            if (sourceAddress != null && AddressUtility.isValidStandardAddress(sourceAddress)) {
                request.sourceAddress = ByteStringAddressUtility.createProperByteStringAutomatically(sourceAddress)
            }
            if (takeFeeFromOutputs != null) {
                request.takeFeeFromOutputs = takeFeeFromOutputs
            }

            val result = cliShell.adminService.sendCoins(request.build())

            prepareResult(result.success, result.resultsList) {
                SendCoinsPayload(result)
            }
        } else {
            // Should never happen; address validity is checked by argument parser
            failure()
        }
    }

    rpcCommand(
        name = "Set Transaction Fee",
        form = "settxfee",
        description = "Set the transaction fee for future transactions",
        parameters = listOf(
            CommandParameter(name = "transactionFee", mapper = CommandParameterMappers.LONG, required = true)
        )
    ) {
        val fee = getParameter<Long>("transactionFee")
        val request = RpcSetTransactionFeeRequest.newBuilder().setAmount(fee).build()
        val result = cliShell.adminService.setTransactionFee(request)

        prepareResult(result.success, result.resultsList)
    }

    rpcCommand(
        name = "Make Unsigned Multisig Tx",
        form = "makeunsignedmultisigtx|unsignedmultisigtx|makemultisigtx|multisigtx|generateunsignedmultisigtx",
        description = "(Generates an unsigned multisig transaction)",
        parameters = listOf(
            CommandParameter(name = "sourceAddress", mapper = ShellCommandParameterMappers.MULTISIG_ADDRESS, required = true),
            CommandParameter(name = "amount", mapper = CommandParameterMappers.STRING, required = true),
            CommandParameter(name = "destinationAddress", mapper = ShellCommandParameterMappers.STANDARD_OR_MULTISIG_ADDRESS, required = true),
            CommandParameter(name = "transactionFee", mapper = CommandParameterMappers.STRING, required = false),
            CommandParameter(name = "signatureIndex", mapper = CommandParameterMappers.INTEGER, required = false)
        ),
        suggestedCommands = { listOf("getbalance", "gethistory", "generatemultisigaddress", "submitmultisigtx") }
    ) {
        val amount: String = getParameter("amount")
        val atomicAmount = Utility.convertDecimalCoinToAtomicLong(amount)
        val destinationAddress: String = getParameter("destinationAddress")
        val sourceAddress: String = getParameter("sourceAddress")
        val transactionFee: String? = getOptionalParameter("transactionFee")
        val signatureIndex: Int? = getOptionalParameter("signatureIndex")

        val request = RpcMakeUnsignedMultisigTxRequest.newBuilder()
            .setSourceMultisigAddress(ByteStringAddressUtility.createProperByteStringAutomatically(sourceAddress))
            .addAmounts(RpcOutput.newBuilder()
                .setAddress(ByteStringAddressUtility.createProperByteStringAutomatically(destinationAddress))
                .setAmount(atomicAmount))

        if (transactionFee != null) {
            request.fee = Utility.convertDecimalCoinToAtomicLong(transactionFee)
        }
        if (signatureIndex != null) {
            request.signatureIndexString = ByteString.copyFrom(("" + signatureIndex).toByteArray())
        }

        val result = cliShell.adminService.makeUnsignedMultisigTx(request.build())

        prepareResult(result.success, result.resultsList) {
            MakeUnsignedMultisigTxPayload(result)
        }
    }

    rpcCommand(
        name = "Submit Transaction",
        form = "submittx",
        description = "Submit a raw transaction to be added to the mempool",
        parameters = listOf(
            CommandParameter(name = "rawTransaction", mapper = CommandParameterMappers.STRING, required = true)
        )
    ) {
        val rawTransaction: String = getParameter("rawTransaction")
        val request = RpcSubmitTransactionsRequest
            .newBuilder().addTransactions(RpcTransactionUnion.parseFrom(rawTransaction.asHexBytes()))
            .build()
        val result = cliShell.adminService.submitTransactions(request)

        prepareResult(result.success, result.resultsList)
    }

    rpcCommand(
        name = "Submit Multisig Tx",
        form = "submitmultisigtx",
        description = "(Submits an signed multisig transaction)",
        parameters = listOf(
            CommandParameter(name = "unsignedtransactionhex", mapper = CommandParameterMappers.HEX_STRING, required = true),
            CommandParameter(name = "csvpublickeysoraddresses", mapper = ShellCommandParameterMappers.COMMA_SEPARATED_PUBLIC_KEYS_OR_ADDRESSES, required = true),
            CommandParameter(name = "csvsignatureshex", mapper = ShellCommandParameterMappers.COMMA_SEPARATED_SIGNATURES, required = true)
        ),
        suggestedCommands = { listOf("getbalance", "gethistory", "generatemultisigaddress", "makeunsignedmultisigtx") }
    ) {
        val unsignedTransactionHex: String = getParameter("unsignedtransactionhex")
        val publicKeysOrAddressesStr: String = getParameter("csvpublickeysoraddresses")
        val signaturesHexStr: String = getParameter("csvsignatureshex")

        val unsignedTransactionBytes = unsignedTransactionHex.asHexBytes()

        val publicKeysOrAddresses = publicKeysOrAddressesStr.split(",")
        val signaturesHex = signaturesHexStr.split(",")

        if (publicKeysOrAddresses.size != signaturesHex.size) {
            failure("-1", "Invalid public keys / addresses and signatures provided!", "There must be an equivalent number of provided public keys / addresses as there are signatures. Note that a blank multisig slot (no public key, no slot) ")
        } else {
            var unsignedTransaction: RpcUnsignedMultisigTransactionWithIndex? = null
            try {
                unsignedTransaction = RpcUnsignedMultisigTransactionWithIndex.parseFrom(unsignedTransactionBytes)
            } catch (e: Exception) {
                failure("-1", "Unable to parse the provided raw multisig transaction!", e.message.toString())
            }

            if (unsignedTransaction != null) {
                if (unsignedTransaction.unsignedMultisigTansaction.type != RpcTransaction.Type.MULTISIG) {
                    failure("-1", "Invalid transaction provided!", "The provided transaction is not a multisig transaction!")
                } else {
                    val signedMultisigTxBuilder = RpcSignedMultisigTransaction.newBuilder().apply {
                        signatureIndex = unsignedTransaction.signatureIndex
                        transaction = unsignedTransaction.unsignedMultisigTansaction
                    }

                    val multisigBundleBuilder = RpcMultisigBundle.newBuilder()
                    for (i in publicKeysOrAddresses.indices) {
                        val multisigSlotBuilder = RpcMultisigSlot.newBuilder()
                        if (AddressUtility.isValidStandardAddress(publicKeysOrAddresses[i])) {
                            multisigSlotBuilder.ownerAddress = ByteStringUtility.base58ToByteString(publicKeysOrAddresses[i])
                            multisigSlotBuilder.populated = false
                        } else {
                            if (signaturesHex[i].isEmpty()) {
                                failure("-1", "Invalid signatures provided!", "Slot $i was indicated as populated (public key provided) but there is no corresponding signature!")
                            } else {
                                multisigSlotBuilder.publicKey = publicKeysOrAddresses[i].asHexByteString()
                                multisigSlotBuilder.signature = signaturesHex[i].asHexByteString()
                                multisigSlotBuilder.populated = true
                            }
                        }
                        multisigBundleBuilder.addSlots(i, multisigSlotBuilder.build())
                    }
                    signedMultisigTxBuilder.signatureBundle = multisigBundleBuilder.build()

                    val request = RpcSubmitMultisigTxRequest.newBuilder().apply {
                        multisigTransaction = signedMultisigTxBuilder.build()
                    }
                    val result = cliShell.adminService.submitMultisigTx(request.build())

                    prepareResult(result.success, result.resultsList)
                }
            } else {
                failure("-1", "Invalid unsignedTransactionBytes", "Unable to parse the provided unsignedTransactionBytes")
            }
        }
    }
}
