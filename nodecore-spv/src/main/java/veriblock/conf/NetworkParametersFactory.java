package veriblock.conf;

public class NetworkParametersFactory {

    public static NetworkParameters get(String network){
        if(MainNetParameters.NETWORK.equalsIgnoreCase(network)){
            return MainNetParameters.get();
        } else if (TestNetParameters.NETWORK.equalsIgnoreCase(network)){
            return TestNetParameters.get();
        } else if(AlphaNetParameters.NETWORK.equalsIgnoreCase(network)) {
            return AlphaNetParameters.get();
        } else {
            throw new IllegalArgumentException("network parameter is wrong.");
        }

    }
}
