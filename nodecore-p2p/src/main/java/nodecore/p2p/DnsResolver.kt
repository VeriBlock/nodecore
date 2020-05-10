// VeriBlock NodeCore
// Copyright 2017-2020 Xenios SEZC
// All rights reserved.
// https://www.veriblock.org
// Distributed under the MIT software license, see the accompanying
// file LICENSE or http://www.opensource.org/licenses/mit-license.php.

package nodecore.p2p;

import org.xbill.DNS.Lookup;
import org.xbill.DNS.Record;
import org.xbill.DNS.TextParseException;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

public class DnsResolver {
    public DnsResolver() {}

    public List<String> query(String dns) throws TextParseException {
        Lookup dnsLookup = new Lookup(dns);
        Record[] aRecords = dnsLookup.run();
        if (aRecords != null && aRecords.length > 0) {
            return Arrays.stream(aRecords).map(Record::rdataToString).collect(Collectors.toList());
        }

        return Collections.emptyList();
    }
}
