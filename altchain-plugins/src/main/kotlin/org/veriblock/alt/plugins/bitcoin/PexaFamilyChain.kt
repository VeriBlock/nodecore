// VeriBlock Blockchain Project
// Copyright 2017-2018 VeriBlock, Inc
// Copyright 2018-2021 Xenios SEZC
// All rights reserved.
// https://www.veriblock.org
// Distributed under the MIT software license, see the accompanying
// file LICENSE or http://www.opensource.org/licenses/mit-license.php.

package org.veriblock.alt.plugins.bitcoin

import org.veriblock.core.altchain.AltchainPoPEndorsement
import org.veriblock.core.contracts.BlockEvidence
import org.veriblock.core.crypto.*
import org.veriblock.core.utilities.SerializerUtility
import org.veriblock.core.utilities.extensions.flip
import org.veriblock.sdk.alt.plugin.PluginConfig
import org.veriblock.sdk.alt.plugin.PluginSpec
import java.nio.ByteBuffer

@PluginSpec(name = "PexaFamily", key = "phx")
class PexaFamilyChain(
    override val key: String,
    configuration: PluginConfig
) : BitcoinFamilyChain(key, configuration) {

    private val crypto = Crypto()

    override fun extractBlockEvidence(altchainPopEndorsement: AltchainPoPEndorsement): BlockEvidence {
        val hash = crypto.SHA256D(altchainPopEndorsement.getHeader()).flip()
        val previousHash = altchainPopEndorsement.getHeader().copyOfRange(4, 36).flip()
        val contextBuffer = ByteBuffer.wrap(altchainPopEndorsement.getContextInfo())
        val height = contextBuffer.getInt()
        val previousKeystone = SerializerUtility.readSingleByteLenValue(contextBuffer, 8, 64).flip()
        val secondPreviousKeystone = SerializerUtility.readSingleByteLenValue(contextBuffer, 8, 64).flip()

        return BlockEvidence(height, hash, previousHash, previousKeystone, secondPreviousKeystone)
    }
}
