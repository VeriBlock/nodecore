// VeriBlock NodeCore
// Copyright 2017-2019 Xenios SEZC
// All rights reserved.
// https://www.veriblock.org
// Distributed under the MIT software license, see the accompanying
// file LICENSE or http://www.opensource.org/licenses/mit-license.php.

package nodecore.miners.pop.api.models;

import nodecore.miners.pop.common.Utility;
import nodecore.miners.pop.contracts.*;

import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

public class ResponseMapper {
    public static OperationDetailResponse map(PreservedPoPMiningOperationState state) {
        OperationDetailResponse response = new OperationDetailResponse();
        response.operationId = state.operationId;
        response.status = state.status.name();
        response.currentAction = state.currentAction.name();
        response.popData = map(state.miningInstruction);
        response.transaction = mapHex(state.transaction);
        response.submittedTransactionId = state.submittedTransactionId;
        response.bitcoinBlockHeaderOfProof = mapHex(state.bitcoinBlockHeaderOfProof);
        response.bitcoinContextBlocks = mapHex(state.bitcoinContextBlocks);
        response.merklePath = state.merklePath;
        response.alternateBlocksOfProof = mapHex(state.alternateBlocksOfProof);
        response.detail = state.detail;
        response.popTransactionId = state.popTransactionId;

        return response;
    }

    public static PoPDataResponse map(PoPMiningInstruction miningInstruction) {
        PoPDataResponse response = new PoPDataResponse();
        response.publicationData = mapHex(miningInstruction.publicationData);
        response.endorsedBlockHeader = mapHex(miningInstruction.endorsedBlockHeader);
        response.minerAddress = mapBase58(miningInstruction.minerAddress);

        return response;
    }

    public static OperationSummaryResponse map(OperationSummary summary) {
        OperationSummaryResponse response = new OperationSummaryResponse();
        response.operationId = summary.getOperationId();
        response.endorsedBlockNumber = summary.getEndorsedBlockNumber();
        response.state = summary.getState();
        response.action = summary.getAction();
        response.message = summary.getMessage();

        return response;
    }

    public static ResultResponse map(Result result) {
        ResultResponse response = new ResultResponse();
        response.failed = result.didFail();
        response.messages = result.getMessages().stream()
                .map(ResponseMapper::map)
                .collect(Collectors.toList());

        return response;
    }

    public static MineResultResponse map(MineResult result) {
        MineResultResponse response = new MineResultResponse();
        response.operationId = result.getOperationId();
        response.failed = result.didFail();
        response.messages = result.getMessages().stream()
                .map(ResponseMapper::map)
                .collect(Collectors.toList());

        return response;
    }

    public static ResultMessageResponse map(ResultMessage message) {
        ResultMessageResponse response = new ResultMessageResponse();
        response.code = message.getCode();
        response.message = message.getMessage();
        response.details = message.getDetails();
        response.error = message.isError();

        return response;
    }

    public static String mapHex(byte[] data) {
        if (data == null) return null;

        return Utility.bytesToHex(data);
    }

    public static List<String> mapHex(Collection<byte[]> data) {
        if (data == null) return null;

        return data.stream().map(ResponseMapper::mapHex).collect(Collectors.toList());
    }

    public static String mapBase58(byte[] data) {
        if (data == null) return null;

        return Utility.bytesToBase58(data);
    }
}
