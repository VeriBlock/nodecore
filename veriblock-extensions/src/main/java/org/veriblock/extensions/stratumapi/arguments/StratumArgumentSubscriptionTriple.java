// VeriBlock NodeCore
// Copyright 2017-2020 Xenios SEZC
// All rights reserved.
// https://www.veriblock.org
// Distributed under the MIT software license, see the accompanying
// file LICENSE or http://www.opensource.org/licenses/mit-license.php.

package org.veriblock.extensions.stratumapi.arguments;

import com.google.gson.JsonArray;
import org.veriblock.core.utilities.Utility;

/**
 * EthereumStratum/1.0.0 describes Stratum subscriptions using a triple containing a subscription type, id, and protocol.
 * For example, in response to a successful mining.subscribe command, the server will reply with a response containing at least one
 * mining subscription triple. One of these triples will generally contain the mining.notify type.
 */
public class StratumArgumentSubscriptionTriple extends StratumArgument {

    private final StratumArgument.StratumType type = StratumType.SUBSCRIPTION_TRIPLE;

    private final String subscriptionType;

    private final String subscriptionId;

    private final String subscriptionProtocol;

    public void throwValidationError(String data) {
        throw new IllegalArgumentException("\"" + data + "\" did not pass the preliminary validation of "
            + getClass().getCanonicalName() + " (" + type.getPreliminaryValidationPattern() + ")");
    }

    /**
     * Constructor for parsing the serialized data type, useful when parsing a command.
     * @param data
     */
    public StratumArgumentSubscriptionTriple(String data) {
        if (data == null) {
            throw new IllegalArgumentException(getClass().getCanonicalName() + "'s constructor cannot be called with null data!");
        }

        // Check the data against the initial sanity checks built into the type enum
        if (!type.preliminaryValidation(data)) {
            throwValidationError(data);
        }

        String[] parts = data.split(",");

        if (parts.length != 3) {
            throw new IllegalArgumentException(getClass().getCanonicalName() + "'s constructor cannot be called with a subscription triple without"
                + " exactly three parts (" + parts.length + ") provided: " + data + "!");
        }

        String potentialSubscriptionType = parts[0];
        if (potentialSubscriptionType.indexOf('\"') != -1) {
            potentialSubscriptionType = potentialSubscriptionType.substring(potentialSubscriptionType.indexOf('\"') + 1);
            if (potentialSubscriptionType.indexOf('\"') != -1) {
                potentialSubscriptionType = potentialSubscriptionType.substring(0, potentialSubscriptionType.indexOf("\""));
            } else {
                throw new IllegalArgumentException(getClass().getCanonicalName() + "'s constructor cannot be called with a subscription type"
                    + " without closing quotes! Invalid subscription type: " + parts[0] + "!");
            }
        } else {
            throw new IllegalArgumentException(getClass().getCanonicalName() + "'s constructor cannot be called with a subscription type"
                + " without opening quotes! Invalid subscription type: " + parts[0] + "!");
        }

        subscriptionType = potentialSubscriptionType;

        String potentialSubscriptionId = parts[1];
        if (potentialSubscriptionId.indexOf('\"') != -1) {
            potentialSubscriptionId = potentialSubscriptionId.substring(potentialSubscriptionId.indexOf('\"') + 1);
            if (potentialSubscriptionId.indexOf('\"') != -1) {
                potentialSubscriptionId = potentialSubscriptionId.substring(0, potentialSubscriptionId.indexOf("\""));
            } else {
                throw new IllegalArgumentException(getClass().getCanonicalName() + "'s constructor cannot be called with a subscription id"
                    + " without closing quotes! Invalid subscription id: " + parts[1] + "!");
            }
        } else {
            throw new IllegalArgumentException(getClass().getCanonicalName() + "'s constructor cannot be called with a subscription id"
                + " without opening quotes! Invalid subscription id: " + parts[1] + "!");
        }

        if (!Utility.isHex(potentialSubscriptionId)) {
            throw new IllegalArgumentException(getClass().getCanonicalName() + "'s constructor cannot be called with a subscription id"
            + " that isn't hex-encoded! Invalid subscription id: " + parts[1] + "!");
        }

        subscriptionId = potentialSubscriptionId;

        String potentialSubscriptionProtocol = parts[2];
        if (potentialSubscriptionProtocol.indexOf('\"') != -1) {
            potentialSubscriptionProtocol = potentialSubscriptionProtocol.substring(potentialSubscriptionProtocol.indexOf('\"') + 1);
            if (potentialSubscriptionProtocol.indexOf('\"') != -1) {
                potentialSubscriptionProtocol = potentialSubscriptionProtocol.substring(0, potentialSubscriptionProtocol.indexOf("\""));
            } else {
                throw new IllegalArgumentException(getClass().getCanonicalName() + "'s constructor cannot be called with a subscription protocol"
                    + " without closing quotes! Invalid subscription protocol: " + parts[2] + "!");
            }
        } else {
            throw new IllegalArgumentException(getClass().getCanonicalName() + "'s constructor cannot be called with a subscription protocol"
                + " without opening quotes! Invalid subscription protocol: " + parts[2] + "!");
        }

        subscriptionProtocol = potentialSubscriptionProtocol;
    }

    /**
     * Constructor for the actual represented data type, useful when creating a command.
     * @param subscriptionType
     * @param subscriptionIdHex
     * @param subscriptionProtocol
     */
    public StratumArgumentSubscriptionTriple(String subscriptionType, String subscriptionIdHex, String subscriptionProtocol) {
        this.subscriptionType = subscriptionType;

        if (!Utility.isHex(subscriptionIdHex)) {
            throw new IllegalArgumentException("A StratumArgumentSubscriptionTriple cannot be constructed with a non-hex subscription ID!");
        }

        this.subscriptionId = subscriptionIdHex;

        this.subscriptionProtocol = subscriptionProtocol;
    }

    /**
     * Gets the processed data contained within this argument, which has a Java type appropriate to the Stratum type represented by this argument.
     *
     * @return The processed data
     */
    public String getData() {
        return getSerialized();
    }


    /**
     * Gets the serialized version of this string which could be used to create an identical copy of this object.
     * @return The original data "sent over the wire" used to create this argument, or the serialized version created for sending over the wire.
     */
    @Override
    public String getSerialized() {
        return getJsonArray().toString();
    }

    /**
     * Get the corresponding Stratum type which this class represents
     * @return The corresponding Stratum type represented by this argument implementation
     */
    @Override
    public StratumType getType() {
        return type;
    }

    /**
     * Gets the string representation of this argument's data: passthrough to .toString() for the underlying datatype
     * of the processed data (or the equivalent of the autoboxed version's toString if the processed data is a primitive).
     * @return String representation of the data represented by this argument
     */
    @Override
    public String toString() {
        return getSerialized();
    }

    public JsonArray getJsonArray() {
        JsonArray params = new JsonArray();
        params.add(subscriptionType);
        params.add(subscriptionId);
        params.add(subscriptionProtocol);

        return params;
    }
}
