package veriblock.model;

public enum LedgerProofStatus {
    ADDRESS_EXISTS,
    ADDRESS_DOES_NOT_EXIST,
    ADDRESS_IS_INVALID;


    public static LedgerProofStatus getByOrdinal(int ordinal){
        return LedgerProofStatus.values()[ordinal];
    }

    public boolean isExists(){
        return this == LedgerProofStatus.ADDRESS_EXISTS;
    }
}
