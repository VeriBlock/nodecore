package veriblock.model;

public class BlockHeader {
    private byte [] header;
    private byte [] hash;

    public BlockHeader(byte[] header, byte[] hash) {
        this.header = header;
        this.hash = hash;
    }

    public byte[] getHeader() {
        return header;
    }

    public void setHeader(byte[] header) {
        this.header = header;
    }

    public byte[] getHash() {
        return hash;
    }

    public void setHash(byte[] hash) {
        this.hash = hash;
    }
}
