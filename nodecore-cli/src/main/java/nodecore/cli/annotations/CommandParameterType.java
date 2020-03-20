// VeriBlock NodeCore CLI
// Copyright 2017-2020 Xenios SEZC
// All rights reserved.
// https://www.veriblock.org
// Distributed under the MIT software license, see the accompanying
// file LICENSE or http://www.opensource.org/licenses/mit-license.php.

package nodecore.cli.annotations;

public enum CommandParameterType {
    STRING,
    HEXSTRING,
    HASH,
    PEER,
    STANDARD_ADDRESS,
    MULTISIG_ADDRESS,
    STANDARD_OR_MULTISIG_ADDRESS,
    COMMA_SEPARATED_STANDARD_ADDRESSES,
    COMMA_SEPARATED_PUBLIC_KEYS_OR_ADDRESSES,
    COMMA_SEPARATED_SIGNATURES,
    INTEGER,
    LONG,
    OUTPUT,
    BOOLEAN
}
