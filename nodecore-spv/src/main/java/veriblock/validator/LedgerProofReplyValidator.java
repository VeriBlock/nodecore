package veriblock.validator;

import nodecore.api.grpc.VeriBlockMessages;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.veriblock.extensions.ledger.LedgerProofWithContext;
import veriblock.net.impl.PeerTableImpl;

public class LedgerProofReplyValidator {
    private static final Logger logger = LoggerFactory.getLogger(PeerTableImpl.class);

    public static boolean validate(VeriBlockMessages.LedgerProofReply.LedgerProofResult ledgerProofResult){
        try {
            LedgerProofWithContext ledgerProofWithContext = LedgerProofWithContext.parseFrom(ledgerProofResult.getLedgerProofWithContext());
        } catch (Exception ex){
            logger.warn("LedgerProofWithContext is not valid. " + ex.getMessage(), ex);
            //TODO SPV-67 add ban for peer.
            return false;
        }
        return true;
    }


}
