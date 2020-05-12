// VeriBlock Blockchain Project
// Copyright 2017-2018 VeriBlock, Inc
// Copyright 2018-2019 Xenios SEZC
// All rights reserved.
// https://www.veriblock.org
// Distributed under the MIT software license, see the accompanying
// file LICENSE or http://www.opensource.org/licenses/mit-license.php.

package org.veriblock.sdk.sqlite.tables;

import org.veriblock.sdk.models.AltPublication;
import org.veriblock.sdk.models.VeriBlockPublication;

import java.util.List;

public class PoPTransactionData {
     public String txHash;
     public AltPublication altPublication;
     public List<VeriBlockPublication> veriBlockPublications;

     public PoPTransactionData(String hash, AltPublication altPublication, List<VeriBlockPublication> veriBlockPublications)
     {
          this.txHash = hash;
          this.altPublication = altPublication;
          this.veriBlockPublications = veriBlockPublications;
     }
}
