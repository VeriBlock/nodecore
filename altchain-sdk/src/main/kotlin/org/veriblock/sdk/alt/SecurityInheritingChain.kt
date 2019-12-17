// VeriBlock Blockchain Project
// Copyright 2017-2018 VeriBlock, Inc
// Copyright 2018-2019 Xenios SEZC
// All rights reserved.
// https://www.veriblock.org
// Distributed under the MIT software license, see the accompanying
// file LICENSE or http://www.opensource.org/licenses/mit-license.php.

package org.veriblock.sdk.alt

import org.veriblock.sdk.AltPublication
import org.veriblock.sdk.VeriBlockPublication

interface SecurityInheritingChain {
    fun getChainIdentifier(): Long

    fun shouldAutoMine(): Boolean

    fun shouldAutoMine(blockHeight: Int): Boolean

    fun getBestBlockHeight(): Int

    fun getPublicationData(blockHeight: Int?): PublicationDataWithContext

    fun submit(proofOfProof: AltPublication, veriBlockPublications: List<VeriBlockPublication>): String

    //fun submit(proofOfProof: AltPublication): SubmitResponse

    //fun inform(veriBlockProofOfProof: VeriBlockPublication): SubmitResponse

    fun updateContext(veriBlockPublications: List<VeriBlockPublication>): String {
        TODO()
    }
}
