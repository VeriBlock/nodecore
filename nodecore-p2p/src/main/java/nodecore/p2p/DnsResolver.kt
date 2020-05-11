// VeriBlock NodeCore
// Copyright 2017-2020 Xenios SEZC
// All rights reserved.
// https://www.veriblock.org
// Distributed under the MIT software license, see the accompanying
// file LICENSE or http://www.opensource.org/licenses/mit-license.php.
package nodecore.p2p

import org.xbill.DNS.Lookup
import org.xbill.DNS.Record
import org.xbill.DNS.TextParseException
import java.util.Arrays
import java.util.stream.Collectors

class DnsResolver {
    @Throws(TextParseException::class)
    fun query(dns: String?): List<String> {
        val dnsLookup = Lookup(dns)
        val aRecords = dnsLookup.run()
        return aRecords?.map {
            it.rdataToString()
        } ?: emptyList()
    }
}
