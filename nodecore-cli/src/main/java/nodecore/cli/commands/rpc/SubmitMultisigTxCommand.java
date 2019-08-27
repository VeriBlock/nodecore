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
import nodecore.api.grpc.utilities.ByteStringUtility;
import nodecore.cli.annotations.CommandParameterType;
import nodecore.cli.annotations.CommandSpec;
import nodecore.cli.annotations.CommandSpecParameter;
import nodecore.cli.commands.serialization.FormattableObject;
import nodecore.cli.commands.serialization.SubmitMultisigTxPayload;
import nodecore.cli.contracts.Result;
import nodecore.cli.contracts.*;
import nodecore.cli.utilities.CommandUtility;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.veriblock.core.utilities.AddressUtility;
import org.veriblock.core.utilities.Utility;

import java.util.Arrays;

@CommandSpec(
        name = "Submit Multisig Tx",
        form = "submitmultisigtx",
        description = "(Submits an signed multisig transaction)")
@CommandSpecParameter(name = "unsignedtransactionhex", required = true, type = CommandParameterType.HEXSTRING)
@CommandSpecParameter(name = "csvpublickeysoraddresses", required = true, type = CommandParameterType.COMMA_SEPARATED_PUBLIC_KEYS_OR_ADDRESSES)
@CommandSpecParameter(name = "csvsignatureshex", required = true, type = CommandParameterType.COMMA_SEPARATED_SIGNATURES)
public class SubmitMultisigTxCommand implements Command {
    private static final Logger _logger = LoggerFactory.getLogger(ValidateAddressCommand.class);
    private Configuration _configuration;

    @Inject
    public SubmitMultisigTxCommand(Configuration configuration) {
        _configuration = configuration;
    }

    @Override
    public Result execute(CommandContext context) {
        Result result = new DefaultResult();
        try {
            byte[] unsignedTransactionBytes = Utility.hexToBytes(context.getParameter("unsignedtransactionhex"));
            String publicKeysOrAddressesStr = context.getParameter("csvpublickeysoraddresses");
            String signaturesHexStr = context.getParameter("csvsignatureshex");

            String[] publicKeysOrAddresses = publicKeysOrAddressesStr.split(",");
            String[] signaturesHex = signaturesHexStr.split(",");

            if (publicKeysOrAddresses.length != signaturesHex.length) {
                result.addMessage("-1", "Invalid public keys / addresses and signatures provided!", "There must be an equivalent number of provided public keys / addresses as there are signatures. Note that a blank multisig slot (no public key, no slot) ", true);
            }

            UnsignedMultisigTransactionWithIndex unsignedTransaction = null;
            try {
                unsignedTransaction = UnsignedMultisigTransactionWithIndex.parseFrom(unsignedTransactionBytes);
            } catch (Exception e) {
                result.addMessage("-1", "Unable to parse the provided raw multisig transaction!", e.getMessage(), true);
                return result;
            }

            if (unsignedTransaction.getUnsignedMultisigTansaction().getType() != Transaction.Type.MULTISIG) {
                result.addMessage("-1", "Invalid transaction provided!", "The provided transaction is not a multisig transaction!", true);
                return result;
            }

            SignedMultisigTransaction.Builder signedMultisigTxBuilder = SignedMultisigTransaction.newBuilder();
            signedMultisigTxBuilder.setSignatureIndex(unsignedTransaction.getSignatureIndex());
            signedMultisigTxBuilder.setTransaction(unsignedTransaction.getUnsignedMultisigTansaction());

            MultisigBundle.Builder multisigBundleBuilder = MultisigBundle.newBuilder();

            for (int i = 0; i < publicKeysOrAddresses.length; i++) {
                MultisigSlot.Builder multisigSlotBuilder = MultisigSlot.newBuilder();
                if (AddressUtility.isValidStandardAddress(publicKeysOrAddresses[i])) {
                    multisigSlotBuilder.setOwnerAddress(ByteStringUtility.base58ToByteString(publicKeysOrAddresses[i]));
                    multisigSlotBuilder.setPopulated(false);
                } else {
                    if (signaturesHex[i] == null || signaturesHex[i].length() == 0) {
                        result.addMessage("-1", "Invalid signatures provided!", "Slot " + i + " was indicated as populated (public key provided) but there is no corresponding signature!", true);
                        return result;
                    }

                    multisigSlotBuilder.setPublicKey(ByteStringUtility.hexToByteString(publicKeysOrAddresses[i]));
                    multisigSlotBuilder.setSignature(ByteStringUtility.hexToByteString(signaturesHex[i]));
                    multisigSlotBuilder.setPopulated(true);
                }

                multisigBundleBuilder.addSlots(i, multisigSlotBuilder.build());
            }
            signedMultisigTxBuilder.setSignatureBundle(multisigBundleBuilder.build());

            SubmitMultisigTxRequest.Builder request = SubmitMultisigTxRequest.newBuilder();

            request.setMultisigTransaction(signedMultisigTxBuilder.build());

            SubmitMultisigTxReply reply = context
                    .adminService()
                    .submitMultisigTx(request.build());


            if (!reply.getSuccess()) {
                result.fail();
            } else {
                FormattableObject<SubmitMultisigTxPayload> temp = new FormattableObject<>(reply.getResultsList());
                temp.success = !result.didFail();
                temp.payload = new SubmitMultisigTxPayload(reply);

                context.outputObject(temp);

                context.suggestCommands(Arrays.asList(
                        GetBalanceCommand.class,
                        GetHistoryCommand.class,
                        GenerateMultisigAddressCommand.class,
                        MakeUnsignedMultisigTxCommand.class
                ));
            }
            for (nodecore.api.grpc.Result r : reply.getResultsList())
                result.addMessage(r.getCode(), r.getMessage(), r.getDetails(), r.getError());
        } catch (StatusRuntimeException e) {
            CommandUtility.handleRuntimeException(result, e, _logger);
        }

        return result;
    }
}
