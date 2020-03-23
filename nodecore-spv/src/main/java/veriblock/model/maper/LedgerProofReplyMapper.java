package veriblock.model.maper;

import nodecore.api.grpc.VeriBlockMessages;
import org.veriblock.sdk.models.Address;
import veriblock.exception.SpvProcessException;
import veriblock.model.BlockHeader;
import veriblock.model.LedgerContext;
import veriblock.model.LedgerProofStatus;
import veriblock.model.LedgerValue;
import veriblock.util.AddressFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class LedgerProofReplyMapper {

    public static List<LedgerContext> map(List<VeriBlockMessages.LedgerProofReply.LedgerProofResult> ledgerProofResults){
        List<LedgerContext> ledgerContexts = new ArrayList<>();

        for (VeriBlockMessages.LedgerProofReply.LedgerProofResult ledgerProofResult : ledgerProofResults) {
            ledgerContexts.add(map(ledgerProofResult));
        }

        return ledgerContexts;
    }

    public static LedgerContext map(VeriBlockMessages.LedgerProofReply.LedgerProofResult ledgerProofResult){
        LedgerContext ledgerContext = new LedgerContext();
        Address address = AddressFactory.build(ledgerProofResult.getAddress().toByteArray());
        LedgerProofStatus status = LedgerProofStatus.getByOrdinal(ledgerProofResult.getResult().getNumber());
        VeriBlockMessages.BlockHeader blockHeaderVB = ledgerProofResult.getLedgerProofWithContext().getBlockHeader();
        BlockHeader blockHeader = new BlockHeader(blockHeaderVB.getHeader().toByteArray(), blockHeaderVB.getHash().toByteArray());

        if(status.isExists()){
            List<LedgerValue> ledgerValues = ledgerProofResult.getLedgerProofWithContext().getLedgerProof().getProofOfExistence()
                    .getVerticalProofLayersList().stream()
                    .map(VeriBlockMessages.LedgerProofNode::getLedgerValue)
                    .map(LedgerProofReplyMapper::map)
                    .collect(Collectors.toList());

            if(ledgerValues.size() < 1){
                throw new SpvProcessException("Ledger proof reply doesn't have ledger value.");
            }

            ledgerContext.setLedgerValue(ledgerValues.get(0));
        }

        ledgerContext.setAddress(address);
        ledgerContext.setLedgerProofStatus(status);
        ledgerContext.setBlockHeader(blockHeader);

        return ledgerContext;
    }

    private static LedgerValue map(VeriBlockMessages.LedgerValue ledgerProofReply){
        return new LedgerValue(ledgerProofReply.getAvailableAtomicUnits(), ledgerProofReply.getFrozenAtomicUnits(),
            ledgerProofReply.getSignatureIndex()
        );
    }


}
