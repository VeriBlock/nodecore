// VeriBlock NodeCore
// Copyright 2017-2020 Xenios SEZC
// All rights reserved.
// https://www.veriblock.org
// Distributed under the MIT software license, see the accompanying
// file LICENSE or http://www.opensource.org/licenses/mit-license.php.

package nodecore.api.ucp.storage;

import org.veriblock.core.types.Pair;
import org.veriblock.core.utilities.Utility;

import java.util.ArrayList;

public class DeltaList {
    private static final char TOP_SEPARATOR = ';';
    private static final char BOTTOM_SEPARATOR = ':';

    private final ArrayList<Pair<Integer, String>> internal;

    public DeltaList(String serialized) {
        if (serialized == null) {
            throw new IllegalArgumentException("DeltaList's constructor cannot be called with a null serialized String!");
        }

        internal = new ArrayList<>();

        if (!serialized.equals("")) {
            String[] pairs = serialized.split("" + TOP_SEPARATOR);
            for (int i = 0; i < pairs.length; i++) {
                String[] pair = pairs[i].split("" + BOTTOM_SEPARATOR);
                if (pair.length != 2) {
                    throw new IllegalArgumentException("A DeltaList pair (" + pairs[i] + ") is not comprised of exactly two sections!");
                }
                if (!Utility.isInteger(pair[0])) {
                    throw new IllegalArgumentException("A DeltaList pair (" + pairs[i] + ") has a non-integer first piece!");
                }
                internal.add(new Pair<Integer, String>(Integer.parseInt(pair[0]), pair[1]));
            }
        }
    }

    public DeltaList(ArrayList<Pair<Integer, String>> pairs) {
        if (pairs == null) {
            throw new IllegalArgumentException("DeltaList constructor cannot be called with a null pairs ArrayList!");
        }
        internal = deepCopy(pairs);
    }

    public int getSize() {
        return internal.size();
    }

    public Pair<Integer, String> getPairAtIndex(int index) {
        if (index < 0) {
            throw new IllegalArgumentException("getPairAtIndex cannot b e called with a negative index!");
        }

        if (index >= getSize()) {
            throw new IllegalArgumentException("getPairAtIndex cannot be called with an index (" + index +
                    ") that is outside the bounds of this DeltaList (" + getSize() + ")!");
        }

        return new Pair<Integer, String>(internal.get(index).getFirst(), internal.get(index).getSecond());
    }

    public String serialize() {
        StringBuilder serialized = new StringBuilder();
        for (int i = 0; i < internal.size(); i++) {
            serialized.append(internal.get(i).getFirst());
            serialized.append(BOTTOM_SEPARATOR);
            serialized.append(internal.get(i).getSecond());

            if (i != internal.size() - 1) {
                serialized.append(TOP_SEPARATOR);
            }
        }

        return serialized.toString();
    }

    public String toString() {
        return serialize();
    }

    private ArrayList<Pair<Integer, String>> deepCopy(ArrayList<Pair<Integer, String>> original) {
        if (original == null) {
            throw new IllegalArgumentException("deepCopy cannot be called with a null input ArrayList!");
        }

        ArrayList<Pair<Integer, String>> copy = new ArrayList<>();
        for (int i = 0; i < original.size(); i++) {
            copy.add(new Pair<Integer, String>(original.get(i).getFirst(), original.get(i).getSecond()));
        }

        return copy;
    }
}
