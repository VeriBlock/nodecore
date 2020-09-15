package org.veriblock.extensions.stratumapi.results;

/**
 * Not all messages sent with Stratum are commands; some are responses to commands.
 */
public class StratumResult {
    public String compileResult() {
        return toString();
    }
}
