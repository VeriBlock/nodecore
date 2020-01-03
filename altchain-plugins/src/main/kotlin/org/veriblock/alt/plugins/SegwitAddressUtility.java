package org.veriblock.alt.plugins;


import org.veriblock.core.utilities.Utility;

public class SegwitAddressUtility {
    private static final String BECH32_CHARSET = "qpzry9x8gf2tvdw0s3jn54khce6mua7l";

    public static void main(String[] args) {
        System.out.println(Utility.bytesToHex(generatePayoutScriptFromSegwitAddress("bcrt1qv6rz22y5xmrv2n2kxmfz7jmdu9adk4myf3ejsw", "bcrt1")));
    }

    public static byte[] generatePayoutScriptFromSegwitAddress(String segwitAddress, String hrp) {
        if (segwitAddress == null) {
            throw new IllegalArgumentException("generatePayoutScriptFromSegwitAddress cannot be called with a null " +
                    "P2PKHAddress!");
        }

        if (hrp == null) {
            throw new IllegalArgumentException("generatePayoutScriptFromSegwitAddress cannot be called with a null " +
                    "human-readable part!");
        }

        if (!segwitAddress.startsWith(hrp)) {
            throw new IllegalArgumentException("generatePayoutScriptFromSegwitAddress cannot be called with an " +
                    "address (" + segwitAddress + ") which doesn't start with the provided hrp String!");
        }

        // P2WPKH: 33 + 6 + hrp.length() + 1
        //  P2WSH: 53 + 6 + hrp.length() + 1
        if ((segwitAddress.length() != (39 + hrp.length() + 1))
                && (segwitAddress.length() != (59 + hrp.length() + 1))) {
            throw new IllegalArgumentException("generatePayoutScriptFromSegwitAddress cannot be called with an " +
                    "address which isn't the length of 39 or 59 + hrp + 1! Provided address: " + segwitAddress +
                    ", hrp: " + hrp);
        }

        if (segwitAddress.charAt(hrp.length()) != '1') {
            throw new IllegalArgumentException("generatePayoutScriptFromSegwitAddress cannot be called with " +
                    "an address that doesn't have the character '1' separating the hrp from the remainder of " +
                    "the address!");
        }

        String addressWithoutHRP = segwitAddress.substring(hrp.length());
        String addressDataSection = addressWithoutHRP.substring(1, addressWithoutHRP.length() - 6); // Remove checksum
        String addressChecksum = segwitAddress.substring(segwitAddress.length() - 6);

        if (addressDataSection.charAt(0) != 'q') {
            throw new IllegalArgumentException("generatePayoutScriptFromSegwitAddress cannot be called with an " +
                    "address that doesn't contain a 'q' immediately after the last '1' in the String!");
        }

        addressDataSection = addressDataSection.substring(1); // Remove initial 'q'

        byte[] decodedData = decodeBech32(addressDataSection);
        byte[] script = new byte[decodedData.length + 2];

        script[0] = (byte) 0x00;
        script[1] = (byte) decodedData.length;

        System.arraycopy(decodedData, 0, script, 2, decodedData.length);

        return script;
    }

    public static byte[] decodeBech32(String bech32String) {
        if (bech32String == null) {
            throw new IllegalArgumentException("decodeBech32 cannot be called with a null bech32 String!");
        }

        if (bech32String.length() == 0) {
            return new byte[]{}; // Return empty byte array
        }

        bech32String = bech32String.toLowerCase();

        // Check to ensure all characters are bech32
        for (int i = 0; i < bech32String.length(); i++) {
            if (!BECH32_CHARSET.contains(("" + bech32String.charAt(i)))) {
                throw new IllegalArgumentException("decodeBech32 cannot be called with a non-bech32 " +
                        "String (" + bech32String + " has character '" + bech32String.charAt(i) + "' at index " +
                        i + "!");
            }
        }

        byte[] decoded = new byte[((bech32String.length() * 5) / 8) + ((bech32String.length() * 5) % 8 == 0 ? 0 : 1)];
        for (int i = 0; i < bech32String.length(); i++) {
            byte decodedValue = (byte)(BECH32_CHARSET.indexOf(bech32String.charAt(i)) & 0x1F);
            int byteIndex = (((i + 1) * 5) / 8);
            switch (i % 8) {
                case 0: decoded[byteIndex] |= ((decodedValue << 3) & 0xFF);
                    break;

                case 1: decoded[byteIndex - 1] |= ((decodedValue >>> 2) & 0x07);
                    decoded[byteIndex] |= ((decodedValue & 0x03) << 6);
                    break;

                case 2: decoded[byteIndex] |= ((decodedValue) << 1);
                    break;

                case 3: decoded[byteIndex - 1] |= ((decodedValue & 0x10) >>> 4);
                    decoded[byteIndex] |= ((decodedValue & 0x0F) << 4);
                    break;

                case 4: decoded[byteIndex - 1] |= ((decodedValue & 0x1E) >> 1);
                    decoded[byteIndex] |= ((decodedValue & 0x01) << 7);
                    break;

                case 5: decoded[byteIndex] |= ((decodedValue << 2) & 0x7C);
                    break;

                case 6: decoded[byteIndex - 1] |= ((decodedValue & 0x18) >> 3);
                    decoded[byteIndex] |= ((decodedValue & 0x07) << 5);
                    break;

                case 7: decoded[byteIndex - 1] |= ((decodedValue & 0x1F));
                    break;
            }
        }

        // Chop off an ending zero, if it exists
        if (decoded[decoded.length - 1] == 0x00) {
            byte[] chopped = new byte[decoded.length - 1];
            System.arraycopy(decoded, 0, chopped, 0, chopped.length);
            return chopped;
        } else {
            return decoded;
        }
    }
}
