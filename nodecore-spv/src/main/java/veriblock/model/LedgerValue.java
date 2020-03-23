package veriblock.model;

public class LedgerValue {
    private final long availableAtomicUnits;
    private final long frozenAtomicUnits;
    private final long signatureIndex;

    public LedgerValue(Long availableAtomicUnits, Long frozenAtomicUnits, Long signatureIndex) {
        this.availableAtomicUnits = availableAtomicUnits;
        this.frozenAtomicUnits = frozenAtomicUnits;
        this.signatureIndex = signatureIndex;
    }

    public long getAvailableAtomicUnits() {
        return availableAtomicUnits;
    }

    public long getFrozenAtomicUnits() {
        return frozenAtomicUnits;
    }

    public long getSignatureIndex() {
        return signatureIndex;
    }

}
