// VeriBlock NodeCore
// Copyright 2017-2020 Xenios SEZC
// All rights reserved.
// https://www.veriblock.org
// Distributed under the MIT software license, see the accompanying
// file LICENSE or http://www.opensource.org/licenses/mit-license.php.

package nodecore.api.ucp.utilities;

public final class AsciiUtility {
    private AsciiUtility(){}

    /**
     * Determines whether the provided character is a printable ASCII character.
     * @param toTest character to test
     * @return Whether toTest is a printable ASCII character
     */
    public static boolean isPrintableASCII(char toTest) {
        return (int) toTest >= 0x20 && (int) toTest <= 0x7E;
    }

    /**
     * Determines whether the provided character is a printable non-whitespace ASCII character.
     * @param toTest character to test
     * @return Whether toTest is a printable non-whitespace ASCII character
     */
    public static boolean isPrintableASCIIAndNotWhitespace(char toTest) {
        return isPrintableASCII(toTest) && toTest != ' ';
    }

    /**
     * Determines whether the provided String is a printable ASCII character.
     * @param toTest String to test
     * @return Whether toTest is a printable ASCII String
     */
    public static boolean isPrintableASCII(String toTest) {
        for (int i = 0; i < toTest.length(); i++) {
            if (!isPrintableASCII(toTest.charAt(i))) {
                return false;
            }
        }
        return true;
    }

    /**
     * Determines whether the provided String is a printable non-whitespace ASCII character.
     * @param toTest String to test
     * @return Whether toTest is a printable non-whitespace ASCII String
     */
    public static boolean isPrintableASCIIAndNotWhitespace(String toTest) {
        for (int i = 0; i < toTest.length(); i++) {
            if (!isPrintableASCIIAndNotWhitespace(toTest.charAt(i))) {
                return false;
            }
        }
        return true;
    }
}
