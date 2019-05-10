// VeriBlock PoP Miner
// Copyright 2017-2019 Xenios SEZC
// All rights reserved.
// https://www.veriblock.org
// Distributed under the MIT software license, see the accompanying
// file LICENSE or http://www.opensource.org/licenses/mit-license.php.

package nodecore.miners.pop.common;
import javax.xml.bind.DatatypeConverter;

/**
 * A BitcoinMerklePath object represents the path, from a TxID to a Bitcoin merkle root.
 *
 */
public class BitcoinMerklePath 
{
	public byte[][] layers;
	public byte[] bottomData;
	public int bottomDataIndex;
	
	/**
	 * Constructs a BitcoinMerklePath object with the provided layers in internal-endian.
	 * 
	 * @param layers The layers of the merkle path
	 * @param bottomData The TxID that this merkle path authenticates
	 * @param bottomDataIndex The index of the bottomData TxID in the block it came from
	 */
	public BitcoinMerklePath(byte[][] layers, byte[] bottomData, int bottomDataIndex)
	{
		if (layers.length == 0)
		{
			throw new IllegalArgumentException("There must be a nonzero number of layers!");
		}
		
		for (int i = 0; i < layers.length; i++)
		{
			if (layers[i].length != 32)
			{
				throw new IllegalArgumentException("Every step of the tree must be a 256-bit number (32-length byte array)!");
			}
		}
		
		/* Store the data */
		this.layers = layers;
		this.bottomData = bottomData;
		this.bottomDataIndex = bottomDataIndex;
	}
	
	/**
	 * Constructs a BitcoinMerklePath object with the provided compact String representation
	 */
	public BitcoinMerklePath(String compactFormat)
	{
		String[] parts = compactFormat.split(":");
		if (parts.length < 3)
		{
			throw new IllegalArgumentException("The compactFormat string must be in the format: \"bottomIndex:bottomData:layer0:...:layerN\"");
		}
		
		if (!Utility.isPositiveInteger(parts[0]))
		{
			throw new IllegalArgumentException("The compactFormat string must be in the format: \"bottomIndex:bottomData:layer0:...:layerN\"");
		}
		
		for (int i = 1; i < parts.length; i++)
		{
			if (parts[i].length() != 64 || !Utility.isHex(parts[i]))
			{
				throw new IllegalArgumentException("The compactFormat string must be in the format: \"bottomIndex:bottomData:layer0:...:layerN\"");
			}
		}
		
		this.bottomDataIndex = Integer.parseInt(parts[0]);
		this.bottomData = DatatypeConverter.parseHexBinary(parts[1]);
		this.layers = new byte[parts.length - 2][];
		
		for (int i = 2; i < parts.length; i++)
		{
			this.layers[i - 2] = DatatypeConverter.parseHexBinary(parts[i]);
		}
	}
	
	/**
	 * Returns the Merkle root produced by following the layers up to the top of the tree.
	 * 
	 * @return The Merkle root produced by following the path up to the top of the transaction tree, encoded in hexadecimal
	 */
	public String getMerkleRoot()
	{
		Crypto crypto = new Crypto();
		
		byte[] movingHash = new byte[32];
		
		/* Account for the first layer's hash being an existing TxID */
		System.arraycopy(bottomData, 0, movingHash, 0, 32);
		
		int layerIndex = bottomDataIndex;
		for (int i = 0; i < layers.length; i++)
		{
			/* Climb one layer up the tree by concatenating the current state with the next layer in the right order */
			movingHash = crypto.SHA256D(Utility.concat((layerIndex % 2 == 0) ? movingHash : layers[i], (layerIndex % 2 == 0) ? layers[i] : movingHash));
			
			/* The position above on the tree will be floor(currentIndex / 2) */
			layerIndex /= 2;
		}
		
		return DatatypeConverter.printHexBinary(Utility.flip(movingHash));
	}
	
	/**
	 * Returns a compact representation of this BitcoinMerklePath. For the purposes of alpha debugging, the path steps are stored in Hex.
	 * Format: bottomIndex:bottomData:layer0:...:layerN
	 * @return A compact representation of this BitcoinmerklePath!
	 */
	public String getCompactFormat()
	{
		String path = this.bottomDataIndex + ":" + DatatypeConverter.printHexBinary(this.bottomData);
		
		for (int i = 0; i < layers.length; i++)
		{
			path += ":" + DatatypeConverter.printHexBinary(layers[i]);
		}
		
		return path;
	}
}
