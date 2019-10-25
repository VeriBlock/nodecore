// VeriBlock PoP Miner
// Copyright 2017-2019 Xenios SEZC
// All rights reserved.
// https://www.veriblock.org
// Distributed under the MIT software license, see the accompanying
// file LICENSE or http://www.opensource.org/licenses/mit-license.php.

package nodecore.miners.pop.common;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * The BitcoinMerkleTree class provides a variety of ways to interact with Bitcoin transaction merkle trees.
 * Bitcoin transaction merkle trees make proving a given transaction exists in a given block incredibly efficient, since
 * only the path up the tree (which grows logarithmically with relation to the number of total entries in the tree).
 * 
 * All methods in this code accept and return data in network-endian order. However, all appropriate endian switching
 * required (and by some considered to be a Bitcoin design flaw) is handled internally, so that data input into
 * a BitcoinMerkleTree and data returned from a BitcoinMerkleTree will always be consistent with the data as they
 * appear when querying Bitcoin using standard RPC. 
 * 
 * Merkle tree calculation can be lazy or eager, but requesting a merkle path will, by necessity, require the entire
 * tree to be built, so it is generally advisable to request eager computation of the merkle tree, which stores it in a
 * local cache to speed up future queries. The first time a merkle path is requested, if the tree has not already been
 * computed, the tree will be computed and cached for subsequent access.
 *
 */
public class BitcoinMerkleTree 
{
	private ArrayList<MerkleLayer> layers = new ArrayList<MerkleLayer>();
	
	private boolean builtTree = false;

	private void buildTree()
	{
		/* When the top layer has a single element, the tree is finished */
		while (layers.get(layers.size() - 1).numElementsInLayer() > 1)
		{
			/* Create the next layer, save it above the current highest layer */
			layers.add(layers.get(layers.size() -1).createNextLayer());
		}
		
		/* Tree is built, set this to true */
		builtTree = true;
	}

	/**
	 * Construct a BitcoinMerkleTree given the provided List<String> of ordered txIDs.
	 * @param evaluateEagerly Whether or not to compute the entire tree now, or when next needed
	 * @param txIDs All of the TxIDs included in the transaction merkle tree from Bitcoin
	 */
	public BitcoinMerkleTree(boolean evaluateEagerly, List<String> txIDs)
	{
		if (txIDs.size() % 2 == 1) {
			txIDs.add(txIDs.get(txIDs.size() - 1));
		}

		/* All of the data, in internal-endian-order, of the tree */
		byte[][] floorData = new byte[txIDs.size()][];
		
		/* Convert all of the TxIDs to byte[]s, flip them for the correct endianness */
		for (int i = 0; i < txIDs.size(); i++)
		{
			floorData[i] = Utility.flip(Utility.hexToBytes(txIDs.get(i)));
		}
		
		/* Create, at a minimum, the bottom floor */
		layers.add(new MerkleLayer(floorData));
		
		if (evaluateEagerly)
		{
			buildTree();
		}
	}
	
	/**
	 * Construct a BitcoinMerkleTree given the provided String[] of ordered txIDs.
	 * @param evaluateEagerly Whether or not to compute the entire tree now, or when next needed
	 * @param txIDs All of the TxIDs included in the transaction merkle tree from Bitcoin
	 */
	public BitcoinMerkleTree(boolean evaluateEagerly, String[] txIDs)
	{
		this(evaluateEagerly, Arrays.asList(txIDs));
	}
	
	/**
	 * Returns the merkle root of this tree, in network-endian-order (as would be seen in Bitcoin-RPC responses).
	 * @return The merkle root of this tree, in network-endian-order (as would be seen in Bitcoin-RPC responses)
	 */
	public String getMerkleRoot()
	{
		/* Build the tree if it hasn't already been done */
		if (!builtTree)
		{
			buildTree();
		}
		
		/* Get the (only) element from the top layer, flip it, convert to hex */
		return Utility.bytesToHex(Utility.flip(layers.get(layers.size() - 1).getElement(0)));
	}
	
	/**
	 * Returns the BitcoinMerklePath which allows a given txID, provided in network-order hexadecimal String format,
	 * to be mapped up to the transaction merkle root.
	 * @param txID The transaction ID, in network-order hexadecimal String format, to get the merkle path for
	 * @return The BitcoinMerklePath which allows the provided txID to be authenticated, null if this txID is not part of this transaction tree
	 */
	public BitcoinMerklePath getPathFromTxID(String txID)
	{
		int foundIndex = 0;
		
		/* The stored TxID will be in reversed-byte-order from the network-byte-order used by Bitcoin-RPC */
		byte[] txIDBytes = Utility.flip(Utility.hexToBytes(txID));
		
		MerkleLayer bottomLayer = layers.get(0);
		for (; foundIndex < bottomLayer.numElementsInLayer(); foundIndex++)
		{
			/* Found a matching TxID in the bottom layer of the tree, where all TxIDs are stored */
			if (Utility.byteArraysAreEqual(bottomLayer.getElement(foundIndex), txIDBytes))
			{
				break;
			}
		}
		
		/* The TxID provided is not part of this tree */
		if (foundIndex == bottomLayer.numElementsInLayer())
		{
			return null;
		}
		
		/* Save the index, since it'll be manipulated layer for calculating the correct corresponding node at each tree layer */
		int indexAtBottom = foundIndex;
		
		/* Path will not include the merkle root */
		byte[][] path = new byte[layers.size() - 1][];
		
		/* Fill up the path with the corresponding nodes */
		for (int i = 0; i < path.length; i++)
		{
			int elementIndex = (foundIndex % 2 == 0) ? foundIndex + 1 : foundIndex - 1;
			if (elementIndex == layers.get(i).numElementsInLayer()) {
				elementIndex = elementIndex - 1;
			}
			/* Get the complementary element (left or right) at the next layer */
			path[i] = layers.get(i).getElement(elementIndex);
			
			/* Index in above layer will be floor(foundIndex / 2) */
			foundIndex /= 2;
		}
		
		return new BitcoinMerklePath(path, txIDBytes, indexAtBottom);
	}
	
	
}
