// VeriBlock Blockchain Project
// Copyright 2017-2018 VeriBlock, Inc
// Copyright 2018-2019 Xenios SEZC
// All rights reserved.
// https://www.veriblock.org
// Distributed under the MIT software license, see the accompanying
// file LICENSE or http://www.opensource.org/licenses/mit-license.php.

package org.veriblock.sdk.blockchain;

import org.veriblock.sdk.blockchain.store.BlockStore;
import org.veriblock.sdk.blockchain.store.StoredBitcoinBlock;
import org.veriblock.sdk.models.BlockStoreException;
import org.veriblock.sdk.models.Sha256Hash;
import org.veriblock.sdk.models.VBlakeHash;
import org.veriblock.sdk.models.VeriBlockBlock;
import org.veriblock.sdk.models.VeriBlockPublication;
import org.veriblock.sdk.services.SerializeDeserializeService;

import java.math.BigInteger;
import java.nio.charset.StandardCharsets;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;

public class VeriBlockPublicationUtilities {
    /**
     * Simplifies a provided list of VeriBlockPublications (which all must endorse VBK blocks in the same keystone period) down to
     * the minimal list of VeriBlockPublications which provide maximum SPV-level VBK security and contain all consensus data
     * (aside from, in some cases, additional VBK headers which are only contained in strictly-worse VeriBlockPublications) expressed
     * by the provided list of VeriBlockPublications.
     *
     * Note that it is possible that simplifyVeriBlockPublications will lose VBK context for forks which don't contain the best
     * available PoP publication for the particular keystone in question. In the future if these VBK forks become
     * dominant, VeriBlockPublications with longer context including that which was previously omitted in favor of another then-better
     * fork must be created.
     *
     * @param publications List of VeriBlockPublications to deduplicate
     * @param bitcoinStore Bitcoin block store to use as a reference for relative Bitcoin block heights
     * @return Simplified/deduplicated list representing the same consensus information as the provided VeriBlockPublication list
     * @throws SQLException 
     * @throws BlockStoreException 
     */
    public static List<VeriBlockPublication> simplifyVeriBlockPublications(List<VeriBlockPublication> publications, BlockStore<StoredBitcoinBlock, Sha256Hash> bitcoinStore) throws BlockStoreException, SQLException {
        if (publications == null) {
            throw new IllegalArgumentException("simplifyVeriBlockPublications cannot be called with a null list!");
        }

        // First, separate all the VeriBlockPublications out based on the keystone(s) it connects to a previous keystone
        // Keystone blocks themselves connect themselves to the previous keystone, and the previous keystone to the 2nd previous
        // Blocks immediately following keystone blocks also connect their preceding keystone block to its previous and 2nd previous
        // All other blocks only connect one keystone to its previous keystone
        HashMap<VBlakeHash, List<VeriBlockPublication>> publicationsByKeystone = new HashMap<>();
        for (VeriBlockPublication publication : publications) {
            List<VBlakeHash> connectedHeaders = getAllContextualizedKeystonesFromHeader(publication.getTransaction().getPublishedBlock());
            for (VBlakeHash connectedHeader : connectedHeaders) {
                publicationsByKeystone.putIfAbsent(connectedHeader, new ArrayList<>());
                publicationsByKeystone.get(connectedHeader).add(publication);
            }
        }

        List<VeriBlockPublication> simplifiedVeriBlockPublications = new ArrayList<>();
        HashSet<String> alreadyAddedVeriBlockPublications = new HashSet<>();

        for (VBlakeHash keystoneForConsideration : publicationsByKeystone.keySet()) {
            List<VeriBlockPublication> competingVeriBlockPublications = publicationsByKeystone.get(keystoneForConsideration);
            VeriBlockPublication bestVeriBlockPublication = getBestVTBWhichConnectsAParticularKeystone(competingVeriBlockPublications, bitcoinStore);
            String UUID = SerializeDeserializeService.getId(bestVeriBlockPublication.getTransaction()).toString() + bestVeriBlockPublication.getContainingBlock().getHash().toString();
            if (!alreadyAddedVeriBlockPublications.contains(UUID)) {
                // Not adding a duplicate (which can happen with a keystone or right-after-a-keystone block is the
                // best endorsement which connects two contiguous keystones
                simplifiedVeriBlockPublications.add(bestVeriBlockPublication);
                alreadyAddedVeriBlockPublications.add(UUID);
            }
        }

        return simplifiedVeriBlockPublications;
    }

    private static List<VBlakeHash> getAllContextualizedKeystonesFromHeader(VeriBlockBlock block) {
        List<VBlakeHash> connectedHeaders = new ArrayList<>();

        // If the header is a keystone block, then it connects itself to the previous keystone, and the previous keystone to the 2nd previous
        if (block.isKeystone()) {
            connectedHeaders.add(block.getHash().trimToPreviousKeystoneSize());
            connectedHeaders.add(block.getEffectivePreviousKeystone());
        }

        // If the header is immediately after a keystone block, then it connects the immediately preceding keystone to its previous,
        // and that previous to the immediately preceding keystone to the 2nd previous.
        else if (block.getHeight() % 20 == 1) {
            connectedHeaders.add(block.getPreviousBlock().trimToPreviousKeystoneSize());
            connectedHeaders.add(block.getPreviousKeystone());
        } else {
            connectedHeaders.add(block.getPreviousKeystone().trimToPreviousKeystoneSize());
        }

        return connectedHeaders;
    }

    /**
     * Algorithm for determining which VTB in the provided list (which all connect a particular keystone) is the best
     *      1. Filter publications to the earliest block of proof (Bitcoin)
     *      2. Filter publications to the earliest endorsed VeriBlock (VeriBlock)
     *      3. Filter publications to the earliest contained VeriBlock (VeriBlock)
     *      4. Select the single publication with the lowest Sha-256 hash of the compact VeriBlock Merkle Tree
     * @param publications The VeriBlockPublications to compare to find the best
     * @param bitcoinStore the Bitcoin block store to reference for relative Bitcoin block indexes
     * @return The best VeriBlockPublications in the provided list
     * @throws SQLException 
     * @throws BlockStoreException 
     */
    private static VeriBlockPublication getBestVTBWhichConnectsAParticularKeystone(List<VeriBlockPublication> publications, BlockStore<StoredBitcoinBlock, Sha256Hash> bitcoinStore) throws BlockStoreException, SQLException {
        List<VeriBlockPublication> bestBlockOfProofPublications = new ArrayList<>();

        // Step 1: Find the publications in the earliest block of proof
        int firstBTCBlockOfPublicationIndex = Integer.MAX_VALUE - 1;
        for (VeriBlockPublication vtb : publications) {
            StoredBitcoinBlock storedBitcoinBlock = bitcoinStore.scanBestChain(vtb.getTransaction().getBlockOfProof().getHash());
            if (storedBitcoinBlock != null) {
                if (storedBitcoinBlock.getHeight() < firstBTCBlockOfPublicationIndex) {
                    bestBlockOfProofPublications.clear();
                    bestBlockOfProofPublications.add(vtb);
                    firstBTCBlockOfPublicationIndex = storedBitcoinBlock.getHeight();
                } else if (storedBitcoinBlock.getHeight() == firstBTCBlockOfPublicationIndex) {
                    bestBlockOfProofPublications.add(vtb);
                }
            }
        }

        // Step 2: Trim it down to endorsements of the earliest VeriBlock block
        List<VeriBlockPublication> earliestVeriBlockPublications = new ArrayList<>();
        int firstVBKBlockPublishedHeight = Integer.MAX_VALUE - 1;
        for (VeriBlockPublication vtb : bestBlockOfProofPublications) {
            VeriBlockBlock block = vtb.getTransaction().getPublishedBlock();
            if (block.getHeight() < firstVBKBlockPublishedHeight) {
                earliestVeriBlockPublications.clear();
                earliestVeriBlockPublications.add(vtb);
                firstVBKBlockPublishedHeight = block.getHeight();
            } else if (block.getHeight() == firstVBKBlockPublishedHeight) {
                earliestVeriBlockPublications.add(vtb);
            }
        }

        // Step 3: Trim the list to the remaining endorsements contained in the earliest VeriBlock block
        List<VeriBlockPublication> earliestContainedPublications = new ArrayList<>();
        int firstVBKBlockHeightContainingProof = Integer.MAX_VALUE - 1;
        for (VeriBlockPublication vtb : earliestVeriBlockPublications) {
            VeriBlockBlock block = vtb.getContainingBlock();
            if (block.getHeight() < firstVBKBlockHeightContainingProof) {
                earliestContainedPublications.clear();
                earliestContainedPublications.add(vtb);
                firstVBKBlockHeightContainingProof = block.getHeight();
            } else if (block.getHeight() == firstVBKBlockHeightContainingProof) {
                earliestContainedPublications.add(vtb);
            }
        }

        // Step 4: Tie-breaker hash calculation
        BigInteger lowest = null;
        VeriBlockPublication best = null;
        for (VeriBlockPublication publication : earliestContainedPublications) {
            Sha256Hash hash = Sha256Hash.of(publication.getMerklePath().toCompactString().getBytes(StandardCharsets.UTF_8));
            if (lowest == null || hash.toBigInteger().compareTo(lowest) < 0) {
                best = publication;
                lowest = hash.toBigInteger();
            }
        }

        return best;
    }
}
