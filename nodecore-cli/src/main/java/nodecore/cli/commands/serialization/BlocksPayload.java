// VeriBlock NodeCore CLI
// Copyright 2017-2019 Xenios SEZC
// All rights reserved.
// https://www.veriblock.org
// Distributed under the MIT software license, see the accompanying
// file LICENSE or http://www.opensource.org/licenses/mit-license.php.

package nodecore.cli.commands.serialization;

import nodecore.api.grpc.Block;

import java.util.ArrayList;
import java.util.List;

public class BlocksPayload {
    public BlocksPayload(final List<Block> list) {
        for (final Block block : list)
            blocks.add(new BlockInfo(block));
    }

    public List<BlockInfo> blocks = new ArrayList<>();
}
