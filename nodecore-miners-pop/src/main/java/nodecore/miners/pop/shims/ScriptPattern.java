// VeriBlock NodeCore
// Copyright 2017-2019 Xenios SEZC
// All rights reserved.
// https://www.veriblock.org
// Distributed under the MIT software license, see the accompanying
// file LICENSE or http://www.opensource.org/licenses/mit-license.php.

/*
 * Copyright 2017 John L. Jegutanis
 * Copyright 2018 Andreas Schildbach
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package nodecore.miners.pop.shims;

import org.bitcoinj.script.Script;
import org.bitcoinj.script.ScriptChunk;
import org.bitcoinj.script.ScriptOpCodes;

import java.util.List;

import static org.bitcoinj.script.ScriptOpCodes.OP_CHECKSIG;
import static org.bitcoinj.script.ScriptOpCodes.OP_DUP;
import static org.bitcoinj.script.ScriptOpCodes.OP_EQUAL;
import static org.bitcoinj.script.ScriptOpCodes.OP_EQUALVERIFY;
import static org.bitcoinj.script.ScriptOpCodes.OP_HASH160;

/**
 * This is a Script pattern matcher with some typical script patterns
 */
public class ScriptPattern {
    public static final int LEGACY_ADDRESS_LENGTH = 20;

    /**
     * Returns true if this script is of the form {@code DUP HASH160 <pubkey hash> EQUALVERIFY CHECKSIG}, ie, payment to an
     * address like {@code 1VayNert3x1KzbpzMGt2qdqrAThiRovi8}. This form was originally intended for the case where you wish
     * to send somebody money with a written code because their node is offline, but over time has become the standard
     * way to make payments due to the short and recognizable base58 form addresses come in.
     */
    public static boolean isPayToPubKeyHash(Script script) {
        List<ScriptChunk> chunks = script.getChunks();
        if (chunks.size() != 5) {
            return false;
        }
        if (!chunks.get(0).equalsOpCode(OP_DUP)) {
            return false;
        }
        if (!chunks.get(1).equalsOpCode(OP_HASH160)) {
            return false;
        }
        byte[] chunk2data = chunks.get(2).data;
        if (chunk2data == null) {
            return false;
        }
        if (chunk2data.length != LEGACY_ADDRESS_LENGTH) {
            return false;
        }
        if (!chunks.get(3).equalsOpCode(OP_EQUALVERIFY)) {
            return false;
        }
        if (!chunks.get(4).equalsOpCode(OP_CHECKSIG)) {
            return false;
        }
        return true;
    }

    /**
     * Extract the pubkey hash from a P2PKH scriptPubKey. It's important that the script is in the correct form, so you
     * will want to guard calls to this method with {@link #isPayToPubKeyHash(Script)}.
     */
    public static byte[] extractHashFromPayToPubKeyHash(Script script) {
        return script.getChunks().get(2).data;
    }

    /**
     * <p>
     * Whether or not this is a scriptPubKey representing a P2SH output. In such outputs, the logic that
     * controls reclamation is not actually in the output at all. Instead there's just a hash, and it's up to the
     * spending input to provide a program matching that hash.
     * </p>
     * <p>
     * P2SH is described by <a href="https://github.com/bitcoin/bips/blob/master/bip-0016.mediawiki">BIP16</a>.
     * </p>
     */
    public static boolean isPayToScriptHash(Script script) {
        List<ScriptChunk> chunks = script.getChunks();
        // We check for the effective serialized form because BIP16 defines a P2SH output using an exact byte
        // template, not the logical program structure. Thus you can have two programs that look identical when
        // printed out but one is a P2SH script and the other isn't! :(
        // We explicitly test that the op code used to load the 20 bytes is 0x14 and not something logically
        // equivalent like {@code OP_HASH160 OP_PUSHDATA1 0x14 <20 bytes of script hash> OP_EQUAL}
        if (chunks.size() != 3) {
            return false;
        }
        if (!chunks.get(0).equalsOpCode(OP_HASH160)) {
            return false;
        }
        ScriptChunk chunk1 = chunks.get(1);
        if (chunk1.opcode != 0x14) {
            return false;
        }
        byte[] chunk1data = chunk1.data;
        if (chunk1data == null) {
            return false;
        }
        if (chunk1data.length != LEGACY_ADDRESS_LENGTH) {
            return false;
        }
        if (!chunks.get(2).equalsOpCode(OP_EQUAL)) {
            return false;
        }
        return true;
    }

    /**
     * Extract the script hash from a P2SH scriptPubKey. It's important that the script is in the correct form, so you
     * will want to guard calls to this method with {@link #isPayToScriptHash(Script)}.
     */
    public static byte[] extractHashFromPayToScriptHash(Script script) {
        return script.getChunks().get(1).data;
    }

    /**
     * Returns whether this script is using OP_RETURN to store arbitrary data.
     */
    public static boolean isOpReturn(Script script) {
        List<ScriptChunk> chunks = script.getChunks();
        return chunks.size() > 0 && chunks.get(0).equalsOpCode(ScriptOpCodes.OP_RETURN);
    }
}
