package org.veriblock.core

import io.kotest.matchers.shouldBe
import org.bouncycastle.jce.provider.BouncyCastleProvider
import org.junit.Test
import org.veriblock.core.Context
import org.veriblock.core.crypto.MerkleRoot
import org.veriblock.core.crypto.PreviousBlockVbkHash
import org.veriblock.core.crypto.PreviousKeystoneVbkHash
import org.veriblock.core.crypto.Sha256Hash
import org.veriblock.core.crypto.TruncatedMerkleRoot
import org.veriblock.core.crypto.VbkHash
import org.veriblock.core.params.defaultMainNetParameters
import org.veriblock.core.params.defaultRegTestProgPoWParameters
import org.veriblock.core.utilities.Utility
import org.veriblock.core.utilities.extensions.toHex
import org.veriblock.sdk.models.Address
import org.veriblock.sdk.models.AltPublication
import org.veriblock.sdk.models.Coin
import org.veriblock.sdk.models.PublicationData
import org.veriblock.sdk.models.VeriBlockBlock
import org.veriblock.sdk.models.VeriBlockMerklePath
import org.veriblock.sdk.models.VeriBlockTransaction
import org.veriblock.sdk.services.SerializeDeserializeService
import java.security.Security

fun String.unhex(): ByteArray {
    return Utility.hexToBytes(this)
}

class AtvTest {
    init {
        Context.set(defaultRegTestProgPoWParameters)
        Security.addProvider(BouncyCastleProvider())
    }

    val publicationData = PublicationData(0, "header bytes".toByteArray(), "payout info bytes".toByteArray(), "context info bytes".toByteArray())
    val signature = "30440220398b74708dc8f8aee68fce0c47b8959e6fce6354665da3ed87a83f708e62aa6b02202e6c00c00487763c55e92c7b8e1dd538b7375d8df2b2117e75acbb9db7deb3c7".unhex()
    val publicKey = "3056301006072a8648ce3d020106052b8104000a03420004de4ee8300c3cd99e913536cf53c4add179f048f8fe90e5adf3ed19668dd1dbf6c2d8e692b1d36eac7187950620a28838da60a8c9dd60190c14c59b82cb90319e".unhex()

    val vbkTx = VeriBlockTransaction(
        type = 1 /* vbk tx*/,
        sourceAddress = Address("V5Ujv72h4jEBcKnALGc4fKqs6CDAPX"),
        sourceAmount = Coin(1000),
        outputs = emptyList(),
        signatureIndex = 7,
        publicationData = publicationData,
        signature = signature,
        publicKey = publicKey,
        networkByte = null
    )

    val vbkMerklePath = VeriBlockMerklePath(
        treeIndex = 1,
        index = 0,
        subject = MerkleRoot("1fec8aa4983d69395010e4d18cd8b943749d5b4f575e88a375debdc5ed22531c".unhex()),
        layers = listOf(
            "0000000000000000000000000000000000000000000000000000000000000000",
            "0000000000000000000000000000000000000000000000000000000000000000"
        )
            .map { Sha256Hash(it.unhex()) }
            .toMutableList()
    )

    val vbkBlock = VeriBlockBlock(
        height = 5000,
        version = 2,
        previousBlock = PreviousBlockVbkHash("449c60619294546ad825af03".unhex()),
        previousKeystone = PreviousKeystoneVbkHash("b0935637860679ddd5".unhex()),
        secondPreviousKeystone = PreviousKeystoneVbkHash("5ee4fd21082e18686e".unhex()),
        merkleRoot = TruncatedMerkleRoot("26bbfda7d5e4462ef24ae02d67e47d78".unhex()),
        timestamp = 1553699059,
        difficulty = 16842752,
        nonce = 1L
    )

    // version = 1
    val atv = AltPublication(
        transaction = vbkTx,
        merklePath = vbkMerklePath,
        blockOfProof = vbkBlock
    )

    val atvSerialized = "0000000101580101166772f51ab208d32771ab1506970eeb664462730b838e0203e800010701370100010c6865616465722062797465730112636f6e7465787420696e666f20627974657301117061796f757420696e666f2062797465734630440220398b74708dc8f8aee68fce0c47b8959e6fce6354665da3ed87a83f708e62aa6b02202e6c00c00487763c55e92c7b8e1dd538b7375d8df2b2117e75acbb9db7deb3c7583056301006072a8648ce3d020106052b8104000a03420004de4ee8300c3cd99e913536cf53c4add179f048f8fe90e5adf3ed19668dd1dbf6c2d8e692b1d36eac7187950620a28838da60a8c9dd60190c14c59b82cb90319e04000000010400000000201fec8aa4983d69395010e4d18cd8b943749d5b4f575e88a375debdc5ed22531c040000000220000000000000000000000000000000000000000000000000000000000000000020000000000000000000000000000000000000000000000000000000000000000041000013880002449c60619294546ad825af03b0935637860679ddd55ee4fd21082e18686e26bbfda7d5e4462ef24ae02d67e47d785c9b90f3010100000000000001"
    val atvId = "c6d96b8e87f3e347aa1d1051bb3af39c8ea60612ced905d11c6f92d7b6bd50f5"

    @Test
    fun atvSerialization() {
        val serialized = SerializeDeserializeService.serialize(atv)
        serialized.toHex() shouldBe atvSerialized.toUpperCase()
    }

    @Test
    fun atvGetId() {
        atv.getId().toHex() shouldBe atvId.toUpperCase()
    }
}
