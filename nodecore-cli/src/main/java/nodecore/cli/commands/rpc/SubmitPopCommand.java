// VeriBlock NodeCore CLI
// Copyright 2017-2019 Xenios SEZC
// All rights reserved.
// https://www.veriblock.org
// Distributed under the MIT software license, see the accompanying
// file LICENSE or http://www.opensource.org/licenses/mit-license.php.

package nodecore.cli.commands.rpc;

import com.google.inject.Inject;
import com.google.protobuf.ByteString;
import io.grpc.StatusRuntimeException;
import nodecore.api.grpc.BitcoinBlockHeader;
import nodecore.api.grpc.ProtocolReply;
import nodecore.api.grpc.SubmitPopRequest;
import nodecore.api.grpc.utilities.ByteStringAddressUtility;
import nodecore.cli.annotations.CommandParameterType;
import nodecore.cli.annotations.CommandSpec;
import nodecore.cli.annotations.CommandSpecParameter;
import nodecore.cli.commands.serialization.EmptyPayload;
import nodecore.cli.commands.serialization.FormattableObject;
import nodecore.cli.contracts.Command;
import nodecore.cli.contracts.CommandContext;
import nodecore.cli.contracts.DefaultResult;
import nodecore.cli.contracts.Result;
import nodecore.cli.utilities.CommandUtility;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.veriblock.core.utilities.Utility;

@CommandSpec(
        name = "Submit Proof-of-Proof",
        form = "submitpop",
        description = "Submit a Proof-of-Proof transaction")
@CommandSpecParameter(name = "endorsedBlockHeader", required = true, type = CommandParameterType.STRING)
@CommandSpecParameter(name = "bitcoinTransaction", required = true, type = CommandParameterType.STRING)
@CommandSpecParameter(name = "bitcoinMerklePathToRoot", required = true, type = CommandParameterType.STRING)
@CommandSpecParameter(name = "bitcoinBlockHeader", required = true, type = CommandParameterType.STRING)
@CommandSpecParameter(name = "address", required = false, type = CommandParameterType.STANDARD_ADDRESS)
public class SubmitPopCommand implements Command {
    private static final Logger _logger = LoggerFactory.getLogger(SubmitPopCommand.class);

    @Inject
    public SubmitPopCommand() {
    }

    @Override
    public Result execute(CommandContext context) {
        Result result = new DefaultResult();

        String endorsedBlockHeader = context.getParameter("endorsedBlockHeader");
        String bitcoinTransaction = context.getParameter("bitcoinTransaction");
        String bitcoinMerklePathToRoot = context.getParameter("bitcoinMerklePathToRoot");
        String bitcoinBlockHeader = context.getParameter("bitcoinBlockHeader");
        String address = context.getParameter("address");

        //TODO: Add context Bitcoin block header parameters

        try {
            SubmitPopRequest.Builder requestBuilder = SubmitPopRequest.newBuilder();
            requestBuilder.setEndorsedBlockHeader(ByteString.copyFrom(Utility.hexToBytes(endorsedBlockHeader)));
            requestBuilder.setBitcoinTransaction(ByteString.copyFrom(Utility.hexToBytes(bitcoinTransaction)));
            requestBuilder.setBitcoinMerklePathToRoot(ByteString.copyFrom(bitcoinMerklePathToRoot.getBytes()));
            requestBuilder.setBitcoinBlockHeaderOfProof(BitcoinBlockHeader
                    .newBuilder().setHeader(ByteString.copyFrom(Utility.hexToBytes(bitcoinBlockHeader)))
                    .build());
            if (address != null)
                requestBuilder.setAddress(ByteStringAddressUtility.createProperByteStringAutomatically(address));

            ProtocolReply reply = context
                    .adminService()
                    .submitPop(requestBuilder.build());
            if (!reply.getSuccess()) {
                result.fail();
            } else {
                FormattableObject<EmptyPayload> temp = new FormattableObject<>(reply.getResultsList());
                temp.success = !result.didFail();
                temp.payload = new EmptyPayload();
                context.outputObject(temp);
            }
            for (nodecore.api.grpc.Result r : reply.getResultsList())
                result.addMessage(r.getCode(), r.getMessage(), r.getDetails(), r.getError());
        } catch (StatusRuntimeException e) {
            CommandUtility.handleRuntimeException(result, e, _logger);
        }

        return result;
    }
}
