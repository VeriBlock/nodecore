package org.veriblock.extensions.stratumapi.utilities;

public final class AsciiUtility {
    private AsciiUtility() {
    }

    public static boolean isPrintableASCII(char toTest) {
        return toTest >= ' ' && toTest <= '~';
    }

    public static boolean isPrintableASCIIAndNotWhitespace(char toTest) {
        return isPrintableASCII(toTest) && toTest != ' ';
    }

    public static boolean isPrintableASCII(String toTest) {
        for(int i = 0; i < toTest.length(); ++i) {
            if (!isPrintableASCII(toTest.charAt(i))) {
                return false;
            }
        }

        return true;
    }

    public static boolean isPrintableASCIIAndNotWhitespace(String toTest) {
        for(int i = 0; i < toTest.length(); ++i) {
            if (!isPrintableASCIIAndNotWhitespace(toTest.charAt(i))) {
                return false;
            }
        }

        return true;
    }
}
