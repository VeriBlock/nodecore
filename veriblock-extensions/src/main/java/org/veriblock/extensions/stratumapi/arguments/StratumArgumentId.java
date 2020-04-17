package org.veriblock.extensions.stratumapi.arguments;


public class StratumArgumentId extends StratumArgument {

    private final StratumArgument.StratumType type = StratumType.ID;

    private final int data;

    public void throwValidationError(String data) {
        throw new IllegalArgumentException("\"" + data + "\" did not pass the preliminary validation of "
            + getClass().getCanonicalName() + " (" + type.getPreliminaryValidationPattern() + ")");
    }

    /**
     * Constructor for parsing the serialized data type, useful when parsing a command.
     * @param data
     */
    public StratumArgumentId(String data) {
        if (data == null) {
            throw new IllegalArgumentException(getClass().getCanonicalName() + "'s constructor cannot be called with null data!");
        }

        // Check the data against the initial sanity checks built into the type enum
        if (!type.preliminaryValidation(data)) {
            throwValidationError(data);
        }

        this.data = Integer.parseInt(data);

        if (this.data < 0) {
            throw new IllegalArgumentException(getClass().getCanonicalName() + "'s constructor cannot be called with a negative ID (" +
                this.data + ")!");
        }
    }

    /**
     * Constructor for the actual represented data type, useful when creating a command.
     * @param data
     */
    public StratumArgumentId(Integer data) {
        String serialized = "" + data;

        // Serializing it and then checking it against preliminaryValidation ensures consistency with the rules defined in the enum
        if (!type.preliminaryValidation(serialized)) {
            throwValidationError(serialized);
        }

        if (data < 0) {
            throw new IllegalArgumentException(getClass().getCanonicalName() + "'s constructor cannot be called with a negative ID (" +
                data + ")!");
        }

        this.data = data;
    }

    /**
     * Gets the processed data contained within this argument, which has a Java type appropriate to the Stratum type represented by this argument.
     *
     * @return The processed data
     */
    public int getData() {
        return data;
    }


    /**
     * Gets the serialized version of this string which could be used to create an identical copy of this object.
     * @return The original data "sent over the wire" used to create this argument, or the serialized version created for sending over the wire.
     */
    @Override
    public String getSerialized() {
        return "" + data;
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
        return ((Integer)data).toString();
    }
}
