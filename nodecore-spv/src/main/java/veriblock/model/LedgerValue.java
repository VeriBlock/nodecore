package veriblock.model;

public class LedgerValue {
    private Long availableAtomicUnits;
    private Long frozenAtomicUnits;
    private Long signatureIndex;

    public LedgerValue() {
    }

    public LedgerValue(Long availableAtomicUnits, Long frozenAtomicUnits, Long signatureIndex) {
        this.availableAtomicUnits = availableAtomicUnits;
        this.frozenAtomicUnits = frozenAtomicUnits;
        this.signatureIndex = signatureIndex;
    }

    public Long getAvailableAtomicUnits() {
        return availableAtomicUnits;
    }

    public void setAvailableAtomicUnits(Long availableAtomicUnits) {
        this.availableAtomicUnits = availableAtomicUnits;
    }

    public Long getFrozenAtomicUnits() {
        return frozenAtomicUnits;
    }

    public void setFrozenAtomicUnits(Long frozenAtomicUnits) {
        this.frozenAtomicUnits = frozenAtomicUnits;
    }

    public Long getSignatureIndex() {
        return signatureIndex;
    }

    public void setSignatureIndex(Long signatureIndex) {
        this.signatureIndex = signatureIndex;
    }


}
