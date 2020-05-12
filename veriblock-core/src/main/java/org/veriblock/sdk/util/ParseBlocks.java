package org.veriblock.sdk.util;

import org.veriblock.sdk.models.BitcoinBlock;
import org.veriblock.sdk.models.VeriBlockBlock;
import org.veriblock.sdk.services.SerializeDeserializeService;

import java.util.ArrayList;
import java.util.List;

public class ParseBlocks {

	public static List<VeriBlockBlock> parseVeriBlockBlockList(String str) {
        String[] items = str.split(",", 0);

        List<VeriBlockBlock> result = new ArrayList<VeriBlockBlock>(items.length);
        for(String item : items) {
            result.add(SerializeDeserializeService.parseVeriBlockBlock(Utils.decodeHex(item)));
        }

        return result;
    }
	
	public static List<BitcoinBlock> parseBitcoinBlockList(String str) {
        String[] items = str.split(",", 0);

        List<BitcoinBlock> result = new ArrayList<BitcoinBlock>(items.length);
        for(String item : items) {
            result.add(SerializeDeserializeService.parseBitcoinBlock(Utils.decodeHex(item)));
        }

        return result;
    }
}
