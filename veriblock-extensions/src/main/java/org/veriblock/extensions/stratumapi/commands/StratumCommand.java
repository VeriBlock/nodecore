package org.veriblock.extensions.stratumapi.commands;

import org.veriblock.core.types.Pair;
import org.veriblock.extensions.stratumapi.arguments.StratumArgument;
import org.veriblock.extensions.stratumapi.commands.toclient.MiningNotify;
import org.veriblock.extensions.stratumapi.commands.toclient.MiningSetDifficulty;
import org.veriblock.extensions.stratumapi.commands.toclient.MiningSetExtraNonce;
import org.veriblock.extensions.stratumapi.commands.toserver.MiningAuthorize;
import org.veriblock.extensions.stratumapi.commands.toserver.MiningExtraNonceSubscribe;
import org.veriblock.extensions.stratumapi.commands.toserver.MiningHello;
import org.veriblock.extensions.stratumapi.commands.toserver.MiningSubmit;
import org.veriblock.extensions.stratumapi.commands.toserver.MiningSubscribe;

import java.util.ArrayList;

/**
 * The Stratum protocol provides a lightweight mechanism for miners to communicate with NodeCore pool software.
 * <p>
 * VeriBlock Stratum supports a stratum mode compatible with the Ethereum Stratum v1.0.0 specification (stratum2+tcp://[..]).
 */
public class StratumCommand {
    public enum Command {
        MINING_HELLO(
            MiningHello.class,
            "mining.hello",
            new Pair<String, StratumArgument.StratumType>("agent", StratumArgument.StratumType.AGENT),
            new Pair<String, StratumArgument.StratumType>("host", StratumArgument.StratumType.HOST),
            new Pair<String, StratumArgument.StratumType>("port", StratumArgument.StratumType.PORT),
            new Pair<String, StratumArgument.StratumType>("proto", StratumArgument.StratumType.PROTOCOL)
        ),

        MINING_SUBSCRIBE(
            MiningSubscribe.class,
            "mining.subscribe",
            new Pair<String, StratumArgument.StratumType>("agent", StratumArgument.StratumType.AGENT),
            new Pair<>("proto", StratumArgument.StratumType.PROTOCOL)
        ),

        MINING_EXTRA_NONCE_SUBSCRIBE(
            MiningExtraNonceSubscribe.class,
            "mining.extranonce.subscribe"
        ),

        MINING_AUTHORIZE(
            MiningAuthorize.class,
            "mining.authorize",
            new Pair<String, StratumArgument.StratumType>("username", StratumArgument.StratumType.USERNAME),
            new Pair<String, StratumArgument.StratumType>("password", StratumArgument.StratumType.PASSWORD)
        ),

        MINING_NOTIFY(
            MiningNotify.class,
            "mining.notify",
                new Pair<String, StratumArgument.StratumType>("jobid", StratumArgument.StratumType.JOB_ID),
            new Pair<String, StratumArgument.StratumType>("seedhash", StratumArgument.StratumType.SEED_HASH),
            new Pair<String, StratumArgument.StratumType>("headerhash", StratumArgument.StratumType.HEADER_HASH),
            new Pair<String, StratumArgument.StratumType>("", StratumArgument.StratumType.BLOCK_HEIGHT),
            new Pair<String, StratumArgument.StratumType>("cleanjobs", StratumArgument.StratumType.BOOLEAN)
            ),

        MINING_SET_EXTRA_NONCE(
            MiningSetExtraNonce.class,
            "mining.set_extranonce",
            new Pair<String, StratumArgument.StratumType>("extra_nonce", StratumArgument.StratumType.SYNTHETIC_EXTRA_NONCE)
        ),

        MINING_SET_DIFFICULTY(
            MiningSetDifficulty.class,
            "mining.set_difficulty",
            new Pair<String, StratumArgument.StratumType>("difficulty", StratumArgument.StratumType.JOB_ID)
        ),

        MINING_SUBMIT(
        MiningSubmit.class,
        "mining.submit",
        new Pair<String, StratumArgument.StratumType>("username", StratumArgument.StratumType.USERNAME),
        new Pair<String, StratumArgument.StratumType>("jobid", StratumArgument.StratumType.JOB_ID),
        new Pair<String, StratumArgument.StratumType>("nonce", StratumArgument.StratumType.NONCE));


        private final ArrayList<Pair<String, StratumArgument.StratumType>> pattern;
        private final String friendlyName;
        private final Class<? extends StratumCommand> implementingClass;

        @SafeVarargs
        Command(Class<? extends StratumCommand> implementation, String friendlyName, Pair<String, StratumArgument.StratumType>... arguments) {
            this.implementingClass = implementation;

            this.friendlyName = friendlyName;

            ArrayList<Pair<String, StratumArgument.StratumType>> args = new ArrayList<>();

            for (int i = 0; i < arguments.length; i++) {
                args.add(arguments[i]);
            }

            this.pattern = args;
        }

        public ArrayList<Pair<String, StratumArgument.StratumType>> getPattern() {
            return pattern;
        }

        public Class<? extends StratumCommand> getCommandImplementingClass() {
            return implementingClass;
        }

        public String getFriendlyName() {
            return friendlyName;
        }
    }

    public String compileCommand() {
        return toString();
    }
}
