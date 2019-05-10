// VeriBlock PoP Miner
// Copyright 2017-2019 Xenios SEZC
// All rights reserved.
// https://www.veriblock.org
// Distributed under the MIT software license, see the accompanying
// file LICENSE or http://www.opensource.org/licenses/mit-license.php.

package nodecore.miners.pop.common;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

public class Crypto {
	private static final Logger logger = LoggerFactory.getLogger(Crypto.class);
	private MessageDigest sha256;
	
	public Crypto()
	{
		try 
		{
			sha256 = MessageDigest.getInstance("SHA-256");
		} catch (NoSuchAlgorithmException e)
		{
			logger.error(e.getMessage(), e);
		}
	}
	
	public byte[] SHA256D(byte[] input)
	{
		return SHA256ReturnBytes(SHA256ReturnBytes(input));
	}
	
	public byte[] SHA256ReturnBytes(byte[] input)
	{
		return sha256.digest(input);
	}
	

}
