// VeriBlock NodeCore
// Copyright 2017-2020 Xenios SEZC
// All rights reserved.
// https://www.veriblock.org
// Distributed under the MIT software license, see the accompanying
// file LICENSE or http://www.opensource.org/licenses/mit-license.php.

package org.veriblock.core.crypto;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.veriblock.core.utilities.Utility;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

/**
 * The Crypto class provides easy access to SHA256 output and various encoding thereof.
 * <p>
 * TODO: put ECDSA-related cryptography in here as well.
 * <p>
 * Note that a Crypto object can't be used by several callers at the same time, it is not threadsafe.
 *
 * @author Maxwell Sanchez
 */
public class Crypto {
    private static final Logger _logger = LoggerFactory.getLogger(Crypto.class);
    private MessageDigest _sha256;

    public Crypto() {
        try {
            _sha256 = MessageDigest.getInstance("SHA-256");
        } catch (NoSuchAlgorithmException e) {
            _logger.error("Unhandled exception", e);
        }
    }

    public byte[] vBlakeReturnBytes(byte[] input) { return vBlake.hash(input); }

    public String vBlakeReturnHex(byte[] input) { return Utility.bytesToHex(vBlakeReturnBytes(input)); }

    public byte[] SHA256D(byte[] input) {
        return SHA256ReturnBytes(SHA256ReturnBytes(input));
    }

    public byte[] SHA256ReturnBytes(byte[] input) {
        return _sha256.digest(input);
    }

    public byte[] SHA256ReturnBytes(String input) {
        return SHA256ReturnBytes(input.getBytes(StandardCharsets.UTF_8));
    }

    public String SHA256ReturnHex(byte[] input) {
        return Utility.bytesToHex(SHA256ReturnBytes(input));
    }

    public String SHA256ReturnHex(String input) {
        return Utility.bytesToHex(SHA256ReturnBytes(input));
    }

    public String SHA256ReturnBase58(byte[] input) {
        return Utility.bytesToBase58(SHA256ReturnBytes(input));
    }

    public String SHA256ReturnBase58(String input) {
        return Utility.bytesToBase58(SHA256ReturnBytes(input));
    }
}
