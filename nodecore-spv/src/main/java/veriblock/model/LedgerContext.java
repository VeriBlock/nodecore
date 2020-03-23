package veriblock.model;

import org.veriblock.sdk.models.Address;

import javax.annotation.Nonnull;

public class LedgerContext implements Comparable<LedgerContext> {
    private Address address;
    private LedgerValue ledgerValue;
    private LedgerProofStatus ledgerProofStatus;
    private BlockHeader blockHeader;

    public LedgerValue getLedgerValue() {
        return ledgerValue;
    }

    public void setLedgerValue(LedgerValue ledgerValue) {
        this.ledgerValue = ledgerValue;
    }

    public LedgerProofStatus getLedgerProofStatus() {
        return ledgerProofStatus;
    }

    public void setLedgerProofStatus(LedgerProofStatus ledgerProofStatus) {
        this.ledgerProofStatus = ledgerProofStatus;
    }

    public BlockHeader getBlockHeader() {
        return blockHeader;
    }

    public void setBlockHeader(BlockHeader blockHeader) {
        this.blockHeader = blockHeader;
    }

    public Address getAddress() {
        return address;
    }

    public void setAddress(Address address) {
        this.address = address;
    }

    @Override
    public int compareTo(@Nonnull LedgerContext ledger) {
        if(this.getLedgerProofStatus() == null){
            return 1;
        }

        if(this.getLedgerProofStatus() == LedgerProofStatus.ADDRESS_DOES_NOT_EXIST
                && ledger.getLedgerProofStatus() == LedgerProofStatus.ADDRESS_EXISTS){
            return 1;
        }

        if(ledger.getLedgerProofStatus() != LedgerProofStatus.ADDRESS_EXISTS){
            return -1;
        }

        if (this.getLedgerValue().getSignatureIndex() < ledger.getLedgerValue().getSignatureIndex()) {
            return 1;
        } else if (this.getLedgerValue().getSignatureIndex() == ledger.getLedgerValue().getSignatureIndex()
            && this.getLedgerValue().getAvailableAtomicUnits() < ledger.getLedgerValue().getAvailableAtomicUnits()) {
            return 1;
        }

        return -1;
    }

}
