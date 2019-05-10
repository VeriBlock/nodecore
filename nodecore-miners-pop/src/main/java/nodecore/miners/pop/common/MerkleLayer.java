// VeriBlock PoP Miner
// Copyright 2017-2019 Xenios SEZC
// All rights reserved.
// https://www.veriblock.org
// Distributed under the MIT software license, see the accompanying
// file LICENSE or http://www.opensource.org/licenses/mit-license.php.

package nodecore.miners.pop.common;
/**
 * A BitcoinMerkleLayer represents a layer in a BitcoinMerkleTree, and enables the access of elements (byte[] in
 * internal-endian order, not network-endian order) by their index.
 *
 */
public class MerkleLayer
{
	private byte[][] data;
	
	/**
	 * This constructor for a BitcoinMerkleLayer accepts a byte[] of data.
	 * 
	 * @param data The byte[] of the data this layer should store.
	 */
	public MerkleLayer(byte[][] data)
	{
		this.data = data;
	}
	
	/**
	 * Creates the 'next' (higher, and half the size (round up if an odd number of data exist in this layer)) layer of
	 * the Bitcoin merkle tree.
	 * 
	 * @return The next layer of the Bitcoin merkle tree
	 */
	public MerkleLayer createNextLayer()
	{
		/* Create a 2D array for the new layer data that is half the size (round up, if fractional) of this layer's data */
		byte[][] newData = new byte[((data.length % 2 == 0) ? data.length / 2 : data.length / 2 + 1)][];
		
		for (int i = 0; i < newData.length; i++)
		{
			/* Element i of newData is SHA256D of the two corresponding elements beneath it. If only one element is left, use it as both leaves. */
			newData[i] = new Crypto().SHA256D(Utility.concat(data[i * 2], ((data.length != i * 2 + 1) ? data[i * 2 + 1] : data[i * 2])));
		}
		
		return new MerkleLayer(newData);
	}
	
	/**
	 * Returns the number of elements in this layer
	 * 
	 * @return the number of elements in this layer
	 */
	public int numElementsInLayer()
	{
		return data.length;
	}
	
	/**
	 * Returns the element at the provided index (elementNum)
	 * @param elementNum The index of the element to grab
	 * @return A byte[], in internal order, of this layer's element at the provided index (elementNum)
	 */
	public byte[] getElement(int elementNum)
	{
		return data[elementNum];
	}
}