package veriblock.model;

public class AddressCoinsIndex {
    private String address;
    private Long coins;
    private Long index;

    public AddressCoinsIndex(String address, Long coins, Long index) {
        this.address = address;
        this.coins = coins;
        this.index = index;
    }

    public String getAddress() {
        return address;
    }

    public void setAddress(String address) {
        this.address = address;
    }

    public Long getCoins() {
        return coins;
    }

    public void setCoins(Long coins) {
        this.coins = coins;
    }

    public Long getIndex() {
        return index;
    }

    public void setIndex(Long index) {
        this.index = index;
    }
}
