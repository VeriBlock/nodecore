package org.veriblock.extensions.stratumapi.results.toclient;

import com.google.gson.JsonNull;
import com.google.gson.JsonObject;
import org.veriblock.extensions.stratumapi.arguments.StratumArgumentBoolean;
import org.veriblock.extensions.stratumapi.arguments.StratumArgumentError;
import org.veriblock.extensions.stratumapi.arguments.StratumArgumentId;
import org.veriblock.extensions.stratumapi.results.StratumResult;

public class MiningSubmitResult extends StratumResult {
    private final StratumArgumentId id;
    private final StratumArgumentBoolean result;
    private final StratumArgumentError errorString;

    public MiningSubmitResult(StratumArgumentId id,
                                 StratumArgumentBoolean result,
                                 StratumArgumentError errorString) {
        if (id == null) {
            throw new IllegalArgumentException("MiningSubmitResult cannot be constructed with a null id!");
        }

        if (result == null) {
            throw new IllegalArgumentException("MiningSubmitResult cannot be constructed with a null result!");
        }

        this.id = id;

        this.result = result;

        this.errorString = errorString;
    }

    public String compileResult() {
        JsonObject root = new JsonObject();
        root.addProperty("id", id.getData());
        root.addProperty("result", result.getData());

        if (errorString == null){
            root.add("error", JsonNull.INSTANCE);
        } else {
            root.add("error", errorString.getJsonArray());
        }

        return root.toString();
    }
}
