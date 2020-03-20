// VeriBlock NodeCore
// Copyright 2017-2020 Xenios SEZC
// All rights reserved.
// https://www.veriblock.org
// Distributed under the MIT software license, see the accompanying
// file LICENSE or http://www.opensource.org/licenses/mit-license.php.

package nodecore.api.ucp.storage;

import org.veriblock.core.utilities.BlockUtility;
import org.veriblock.core.utilities.Utility;

public class BlockHeaderList {
    private static final char SEPARATOR = ':';

    private final String[] blockHeaders;

    public BlockHeaderList(String blockHeaders) {
        if (blockHeaders == null) {
            throw new IllegalArgumentException("BlockHeaderList cannot be made with a null list of block headers!");
        }

        if (blockHeaders.equals("")) {
            throw new IllegalArgumentException("BlockHeaderList cannot be made with an empty list of block headers!");
        }

        String[] blockHeaderChunks = blockHeaders.split("" + SEPARATOR);

        for (String blockHeaderChunk : blockHeaderChunks) {
            if (!Utility.isHex(blockHeaderChunk)) {
                throw new IllegalArgumentException("BlockHeaderList cannot be made with a header that is not hex-encoded (" + blockHeaderChunk + ")!");
            }

            if (!BlockUtility.isPlausibleBlockHeader(Utility.hexToBytes(blockHeaderChunk))) {
                throw new IllegalArgumentException("BlockHeaderList cannot be made with a header that is invalid (" + blockHeaderChunk + ")!");
            }
        }

        this.blockHeaders = blockHeaderChunks;
    }

    public String[] getBockHeaders() {
        String[] toReturn = new String[blockHeaders.length];
        System.arraycopy(blockHeaders,0, toReturn, 0, blockHeaders.length);

        return toReturn;
    }

    public String serialize() {
        StringBuilder serialized = new StringBuilder();
        for (int i = 0; i < blockHeaders.length; i++) {
            serialized.append(blockHeaders[i]);
            if (i != blockHeaders.length - 1) {
                serialized.append(SEPARATOR);
            }
        }

        return serialized.toString();
    }

    public String toString() {
        return serialize();
    }

    public boolean isEqual(Object o) {
        if (!(o instanceof BlockHeaderList)) {
            return false;
        }

        return ((BlockHeaderList)(o)).serialize().equals(serialize());
    }
}
