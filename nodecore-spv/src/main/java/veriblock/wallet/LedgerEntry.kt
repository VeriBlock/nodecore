// VeriBlock Blockchain Project
// Copyright 2017-2018 VeriBlock, Inc
// Copyright 2018-2020 Xenios SEZC
// All rights reserved.
// https://www.veriblock.org
// Distributed under the MIT software license, see the accompanying
// file LICENSE or http://www.opensource.org/licenses/mit-license.php.
package veriblock.wallet;

import org.veriblock.core.utilities.SerializerUtility;
import org.veriblock.sdk.models.Coin;
import org.veriblock.sdk.models.Sha256Hash;
import org.veriblock.sdk.services.SerializeDeserializeService;
import veriblock.model.AddressFactory;
import veriblock.model.AddressLight;

import java.io.ByteArrayOutputStream;
import java.io.IOException;

public class LedgerEntry {
    public enum Status {
        PENDING(0),
        CONFIRMED(1),
        FINALIZED(2);

        private int value;
        Status(int value) {
            this.value = value;
        }

        public int getValue() {
            return value;
        }

        public static Status forNumber(int value) {
            switch (value) {
                case 0:
                    return PENDING;
                case 1:
                    return CONFIRMED;
                case 2:
                    return FINALIZED;
                default:
                    return null;
            }
        }
    }

    private final Sha256Hash key;
    private final String address;
    private final Sha256Hash txId;
    private final Coin debitAmount;
    private final Coin creditAmount;
    private final long signatureIndex;
    private final int positionIndex;
    private Status status;

    public Sha256Hash getKey() {
        return key;
    }

    public String getAddress() {
        return address;
    }

    public Sha256Hash getTxId() {
        return txId;
    }

    public Coin getDebitAmount() {
        return debitAmount;
    }

    public Coin getCreditAmount() {
        return creditAmount;
    }

    public long getSignatureIndex() {
        return signatureIndex;
    }

    public int getPositionIndex() {
        return positionIndex;
    }

    public Status getStatus() {
        return status;
    }

    public void setStatus(Status status) {
        this.status = status;
    }

    public LedgerEntry(String address,
                       Sha256Hash txId,
                       Coin debitAmount,
                       Coin creditAmount,
                       long signatureIndex,
                       int positionIndex,
                       Status status) {
        this.address = address;
        this.txId = txId;
        this.debitAmount = debitAmount;
        this.creditAmount = creditAmount;
        this.signatureIndex = signatureIndex;
        this.positionIndex = positionIndex;
        this.status = status;

        this.key = generateKey(address, txId, debitAmount, creditAmount, positionIndex);
    }

    public static Sha256Hash generateKey(String address, Sha256Hash txId, Coin debitAmount, Coin creditAmount, Integer positionIndex) {
        AddressLight a = AddressFactory.create(address);

        try (ByteArrayOutputStream stream = new ByteArrayOutputStream()) {
            a.serializeToStream(stream);
            SerializerUtility.writeSingleByteLengthValueToStream(stream, txId.getBytes());
            SerializeDeserializeService.serialize(debitAmount, stream);
            SerializeDeserializeService.serialize(creditAmount, stream);

            SerializerUtility.writeVariableLengthValueToStream(stream, positionIndex);

            return Sha256Hash.wrap(stream.toByteArray());
        } catch (IOException e) {
            // Should not happen
        }

        return Sha256Hash.ZERO_HASH;
    }
}
