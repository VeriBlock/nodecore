package nodecore.cli.annotations;

public enum ModeType {
    STANDARD,
    SPV;

    public boolean isStandard(){
        return ModeType.STANDARD == this;
    }

    public boolean isSPV(){
        return ModeType.SPV == this;
    }
}
