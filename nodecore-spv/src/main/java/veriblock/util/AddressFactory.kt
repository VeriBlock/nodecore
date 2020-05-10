package veriblock.util;

import org.veriblock.sdk.models.Address;
import org.veriblock.sdk.util.Base58;

public class AddressFactory {

    public static Address build(byte[] bytes){
       return new Address(Base58.encode(bytes));
    }

}
