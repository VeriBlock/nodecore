// VeriBlock PoP Miner
// Copyright 2017-2019 Xenios SEZC
// All rights reserved.
// https://www.veriblock.org
// Distributed under the MIT software license, see the accompanying
// file LICENSE or http://www.opensource.org/licenses/mit-license.php.

package nodecore.miners.pop.shell;

import com.diogonunes.jcdp.color.api.Ansi;
import nodecore.miners.pop.contracts.MessageEvent;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

public class StatusOutputMessage {
    private final MessageEvent.Level level;
    private final String message;
    private final List<String> details;

    public MessageEvent.Level getLevel() {
        return level;
    }

    public Ansi.FColor getColor() {
        switch (level) {
            case WARN:
                return Ansi.FColor.YELLOW;
            case ERROR:
                return Ansi.FColor.RED;
            case SUCCESS:
                return Ansi.FColor.GREEN;
            case MINER:
                return Ansi.FColor.CYAN;
        }
        return Ansi.FColor.WHITE;
    }

    public String getMessage() {
        return message;
    }

    public List<String> getDetails() {
        return details;
    }

    public StatusOutputMessage(MessageEvent.Level level, String message, String...details) {
        this.level = level;
        this.message = message;

        if (details != null) {
            this.details = Arrays.stream(details).collect(Collectors.toList());
        } else {
            this.details = new ArrayList<>();
        }
    }
}
