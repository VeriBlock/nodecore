// VeriBlock NodeCore
// Copyright 2017-2019 Xenios SEZC
// All rights reserved.
// https://www.veriblock.org
// Distributed under the MIT software license, see the accompanying
// file LICENSE or http://www.opensource.org/licenses/mit-license.php.

/*
 * Copyright 2013 Google Inc.
 * Copyright 2014 Andreas Schildbach
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

import nodecore.miners.pop.common.Utility;
import org.bitcoinj.core.Address;
import org.bitcoinj.core.Coin;
import org.bitcoinj.core.ECKey;
import org.bitcoinj.core.InsufficientMoneyException;
import org.bitcoinj.core.Transaction;
import org.bitcoinj.core.TransactionConfidence;
import org.bitcoinj.core.TransactionInput;
import org.bitcoinj.core.TransactionOutput;
import org.bitcoinj.script.Script;
import org.bitcoinj.wallet.CoinSelection;
import org.bitcoinj.wallet.CoinSelector;
import org.bitcoinj.wallet.DefaultCoinSelector;
import org.bitcoinj.wallet.SendRequest;
import org.bitcoinj.wallet.Wallet;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.LinkedList;
import java.util.List;

import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.base.Preconditions.checkState;

/**
 * The most current published package of the bitcoinj library at the time this class was authored was 0.14.7. The fee
 * calculation logic from that package was flawed for the specific scenarios that PoP transactions represent. Namely,
 * due to the 0 value nature of a PoP transaction, the fee calculation required a pre-selection of inputs, but when
 * inputs are pre-selected the transaction size is under-estimated by nearly 40%, resulting in a much lower transaction
 * fee than configured by the feePerKB setting. The methods contained in this WalletShim class have been extracted
 * from the bitcoinj Github repository as of 11/1/2018.
 * <p>
 * See https://github.com/bitcoinj/bitcoinj/tree/2992cc16ff2ccd0c42b68469e90d0cc38a1e54d9.
 * <p>
 * The hope is that the next release of the bitcoinj package contains these improvements and the shim can be removed.
 */
public class WalletShim {
    private static final Logger log = LoggerFactory.getLogger(WalletShim.class);

    private static final CoinSelector DEFAULT_SELECTOR = new DefaultCoinSelector();

    public static void completeTx(Wallet wallet, SendRequest req) throws InsufficientMoneyException {
        // Calculate the amount of value we need to import.
        Coin value = Coin.ZERO;
        for (TransactionOutput output : req.tx.getOutputs()) {
            value = value.add(output.getValue());
        }

        log.info("Completing send tx with {} outputs totalling {} and a fee of {}/kB",
                req.tx.getOutputs().size(),
                Utility.formatBTCFriendlyString(value),
                Utility.formatBTCFriendlyString(req.feePerKb));

        // If any inputs have already been added, we don't need to get their value from wallet
        Coin totalInput = Coin.ZERO;
        for (TransactionInput input : req.tx.getInputs()) {
            if (input.getConnectedOutput() != null) {
                totalInput = totalInput.add(input.getConnectedOutput().getValue());
            } else {
                log.warn("SendRequest transaction already has inputs but we don't know how much they are worth - they will be added to fee.");
            }
        }
        value = value.subtract(totalInput);

        // Check for dusty sends and the OP_RETURN limit.
        if (req.ensureMinRequiredFee && !req.emptyWallet) { // Min fee checking is handled later for emptyWallet.
            int opReturnCount = 0;
            for (TransactionOutput output : req.tx.getOutputs()) {
                if (output.isDust()) {
                    throw new Wallet.DustySendRequested();
                }
                if (ScriptPattern.isOpReturn(output.getScriptPubKey())) {
                    ++opReturnCount;
                }
            }
            if (opReturnCount > 1) // Only 1 OP_RETURN per transaction allowed.
            {
                throw new Wallet.MultipleOpReturnRequested();
            }
        }

        // Calculate a list of ALL potential candidates for spending and then ask a coin selector to provide us
        // with the actual outputs that'll be used to gather the required amount of value. In this way, users
        // can customize coin selection policies. The call below will ignore immature coinbases and outputs
        // we don't have the keys for.
        List<TransactionOutput> candidates = wallet.calculateAllSpendCandidates(true, req.missingSigsMode == Wallet.MissingSigsMode.THROW);

        CoinSelection bestCoinSelection;
        TransactionOutput bestChangeOutput = null;
        List<Coin> updatedOutputValues = null;

        // This can throw InsufficientMoneyException.
        FeeCalculation feeCalculation = calculateFee(wallet, req, value, req.ensureMinRequiredFee, candidates);
        bestCoinSelection = feeCalculation.bestCoinSelection;
        bestChangeOutput = feeCalculation.bestChangeOutput;
        updatedOutputValues = feeCalculation.updatedOutputValues;

        for (TransactionOutput output : bestCoinSelection.gathered) {
            req.tx.addInput(output);
        }

        if (updatedOutputValues != null) {
            for (int i = 0; i < updatedOutputValues.size(); i++) {
                req.tx.getOutput(i).setValue(updatedOutputValues.get(i));
            }
        }

        if (bestChangeOutput != null) {
            req.tx.addOutput(bestChangeOutput);
            log.info("  with {} change", Utility.formatBTCFriendlyString(bestChangeOutput.getValue()));
        }

        // Now shuffle the outputs to obfuscate which is the change.
        if (req.shuffleOutputs) {
            req.tx.shuffleOutputs();
        }

        // Now sign the inputs, thus proving that we are entitled to redeem the connected outputs.
        if (req.signInputs) {
            wallet.signTransaction(req);
        }

        // Check size.
        final int size = req.tx.unsafeBitcoinSerialize().length;
        if (size > Transaction.MAX_STANDARD_TX_SIZE) {
            throw new Wallet.ExceededMaxTransactionSize();
        }

        // Label the transaction as being self created. We can use this later to spend its change output even before
        // the transaction is confirmed. We deliberately won't bother notifying listeners here as there's not much
        // point - the user isn't interested in a confidence transition they made themselves.
        req.tx.getConfidence().setSource(TransactionConfidence.Source.SELF);
        // Label the transaction as being a user requested payment. This can be used to render GUI wallet
        // transaction lists more appropriately, especially when the wallet starts to generate transactions itself
        // for internal purposes.
        req.tx.setPurpose(Transaction.Purpose.USER_PAYMENT);
        // Record the exchange rate that was valid when the transaction was completed.
        req.tx.setExchangeRate(req.exchangeRate);
        req.tx.setMemo(req.memo);

        //req.completed = true;
        log.info("  completed: {}", req.tx);
    }

    private static class FeeCalculation {
        // Selected UTXOs to spend
        public CoinSelection bestCoinSelection;
        // Change output (may be null if no change)
        public TransactionOutput bestChangeOutput;
        // List of output values adjusted downwards when recipients pay fees (may be null if no adjustment needed).
        public List<Coin> updatedOutputValues;
    }

    //region Fee calculation code

    private static FeeCalculation calculateFee(Wallet wallet,
                                               SendRequest req,
                                               Coin value,
                                               boolean needAtLeastReferenceFee,
                                               List<TransactionOutput> candidates) throws InsufficientMoneyException {
        FeeCalculation result;
        Coin fee = Coin.ZERO;
        while (true) {
            result = new FeeCalculation();
            Transaction tx = new Transaction(wallet.getParams());
            addSuppliedInputs(wallet, tx, req.tx.getInputs());

            Coin valueNeeded = value;
            valueNeeded = valueNeeded.add(fee);

            for (int i = 0; i < req.tx.getOutputs().size(); i++) {
                TransactionOutput output = new TransactionOutput(wallet.getParams(), tx, req.tx.getOutputs().get(i).bitcoinSerialize(), 0);
                tx.addOutput(output);
            }
            CoinSelector selector = req.coinSelector == null ? DEFAULT_SELECTOR : req.coinSelector;
            // selector is allowed to modify candidates list.
            CoinSelection selection = selector.select(valueNeeded, new LinkedList<>(candidates));
            result.bestCoinSelection = selection;
            // Can we afford this?
            if (selection.valueGathered.compareTo(valueNeeded) < 0) {
                Coin valueMissing = valueNeeded.subtract(selection.valueGathered);
                throw new InsufficientMoneyException(valueMissing);
            }
            Coin change = selection.valueGathered.subtract(valueNeeded);
            if (change.isGreaterThan(Coin.ZERO)) {
                // The value of the inputs is greater than what we want to send. Just like in real life then,
                // we need to take back some coins ... this is called "change". Add another output that sends the change
                // back to us. The address comes either from the request or currentChangeAddress() as a default.
                Address changeAddress = req.changeAddress;
                if (changeAddress == null) {
                    changeAddress = wallet.currentChangeAddress();
                }
                TransactionOutput changeOutput = new TransactionOutput(wallet.getParams(), tx, change, changeAddress);
                if (changeOutput.isDust()) {
                    // Never create dust outputs; if we would, just
                    // add the dust to the fee.
                    // Oscar comment: This seems like a way to make the condition below "if
                    // (!fee.isLessThan(feeNeeded))" to become true.
                    // This is a non-easy to understand way to do that.
                    // Maybe there are other effects I am missing
                    fee = fee.add(changeOutput.getValue());
                } else {
                    tx.addOutput(changeOutput);
                    result.bestChangeOutput = changeOutput;
                }
            }

            for (TransactionOutput selectedOutput : selection.gathered) {
                TransactionInput input = tx.addInput(selectedOutput);
                // If the scriptBytes don't default to none, our size calculations will be thrown off.
                checkState(input.getScriptBytes().length == 0);
            }

            int size = tx.unsafeBitcoinSerialize().length;
            size += estimateBytesForSigning(wallet, selection);

            Coin feePerKb = req.feePerKb;
            if (needAtLeastReferenceFee && feePerKb.compareTo(Transaction.REFERENCE_DEFAULT_MIN_TX_FEE) < 0) {
                feePerKb = Transaction.REFERENCE_DEFAULT_MIN_TX_FEE;
            }
            Coin feeNeeded = feePerKb.multiply(size).divide(1000);

            if (!fee.isLessThan(feeNeeded)) {
                // Done, enough fee included.
                break;
            }

            // Include more fee and try again.
            fee = feeNeeded;
        }
        return result;
    }

    private static void addSuppliedInputs(Wallet wallet, Transaction tx, List<TransactionInput> originalInputs) {
        for (TransactionInput input : originalInputs) {
            tx.addInput(new TransactionInput(wallet.getParams(), tx, input.bitcoinSerialize()));
        }
    }

    private static int estimateBytesForSigning(Wallet wallet, CoinSelection selection) {
        int size = 0;
        for (TransactionOutput output : selection.gathered) {
            try {
                Script script = output.getScriptPubKey();
                ECKey key = null;
                Script redeemScript = null;

                if (ScriptPattern.isPayToPubKeyHash(script)) {
                    key = wallet.findKeyFromPubHash(ScriptPattern.extractHashFromPayToPubKeyHash(script));
                    checkNotNull(key, "Coin selection includes unspendable outputs");
                } else if (ScriptPattern.isPayToScriptHash(script)) {
                    redeemScript = wallet.findRedeemDataFromScriptHash(ScriptPattern.extractHashFromPayToScriptHash(script)).redeemScript;
                    checkNotNull(redeemScript, "Coin selection includes unspendable outputs");
                }
                size += script.getNumberOfBytesRequiredToSpend(key, redeemScript);
            } catch (Exception e) {
                // If this happens it means an output script in a wallet tx could not be understood. That should never
                // happen, if it does it means the wallet has got into an inconsistent state.
                throw new IllegalStateException(e);
            }
        }
        return size;
    }

    private static boolean adjustOutputDownwardsForFee(Wallet wallet,
                                                       Transaction tx,
                                                       CoinSelection coinSelection,
                                                       Coin feePerKb,
                                                       boolean ensureMinRequiredFee) {
        final int size = tx.unsafeBitcoinSerialize().length + estimateBytesForSigning(wallet, coinSelection);
        Coin fee = feePerKb.multiply(size).divide(1000);
        if (ensureMinRequiredFee && fee.compareTo(Transaction.REFERENCE_DEFAULT_MIN_TX_FEE) < 0) {
            fee = Transaction.REFERENCE_DEFAULT_MIN_TX_FEE;
        }
        TransactionOutput output = tx.getOutput(0);
        output.setValue(output.getValue().subtract(fee));
        return !output.isDust();
    }
}
