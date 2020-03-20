// VeriBlock NodeCore
// Copyright 2017-2020 Xenios SEZC
// All rights reserved.
// https://www.veriblock.org
// Distributed under the MIT software license, see the accompanying
// file LICENSE or http://www.opensource.org/licenses/mit-license.php.

package org.veriblock.core.types;

import org.veriblock.core.utilities.Utility;

/**
 * A bitflag is a way to represent many true/false values simultaneously, and easily communicate those values.
 *
 * Useful for collecting multiple errors and for communicating succinctly the enabled/disabled capabilities of an
 * implementation of some protocol.
 *
 * Bitflags can be any length, and positions are considered from right to left (so position #0 is true if the right-most
 * character is a '1'). For example: "01001" has bit flags #0 and #3 turned on.
 */
public class Bitflag {
    private final String bitflag;

    public Bitflag(String bitflag) {
        if (bitflag == null) {
            throw new IllegalArgumentException("The BitFlag constructor can not be called with a null bitflag!");
        }

        if (!Utility.isBitString(bitflag)) {
            throw new IllegalArgumentException("The BitFlag constructor can only be called with a bit string (all " +
                    "characters are either '0' or '1')!");
        }

        this.bitflag = bitflag;
    }

    /**
     * Determines whether the bit at a particular position is enabled. Note that the int position refers to an index:
     *
     * for a bitflag like "00001": index '0' is true (all others are false)
     * for a bitflag like "00010": index '1' is true (all others are false)
     * for a bitflag like "100001": index '0' and index '5' are t rue (all others are false).
     *
     * Note that a position outside of a bitflag is considered to be simply false.
     * For example, "11111" is considered to be false at index 5 (as if it were "011111").
     *
     * @param position Position (from the right) to check
     * @return Whether the position is true or false in the bitflag
     */
    public boolean isEnabled(int position) {
        int flippedPosition = bitflag.length() -1 -position;

        if (bitflag.length() >= flippedPosition) {
            return false; // Out of bounds
        }

        return bitflag.charAt(flippedPosition) == '1';
    }

    public String toString() {
        return bitflag;
    }

    public boolean equals(Object o) {
        if (!(o instanceof Bitflag)) {
            return false;
        }

        return bitflag.equals(((Bitflag) o).bitflag);
    }

    @Override
    public int hashCode() {
        return bitflag.hashCode();
    }
}
