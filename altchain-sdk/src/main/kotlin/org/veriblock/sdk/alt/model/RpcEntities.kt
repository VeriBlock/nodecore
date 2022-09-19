package org.veriblock.sdk.alt.model

import org.veriblock.core.crypto.PreviousBlockVbkHash
import org.veriblock.core.crypto.PreviousKeystoneVbkHash
import org.veriblock.core.crypto.TruncatedMerkleRoot
import org.veriblock.core.crypto.VbkHash
import org.veriblock.sdk.models.VeriBlockBlock


/*
  "chainWork": "000000000000000000000000000000000000000008d6c888b3d44978f2378750",
  "height": 735498,
  "header": {
    "hash": "000000000000000000036d3be8e84af55f0dd5c9d96facd0ada966a01dcf93da",
    "version": 536870912,
    "previousBlock": "00000000000000000007912809a58b56e1e49771a79a444031f6177daeb01b8a",
    "merkleRoot": "c74527a2b7bbd8ba32c48f2bfa784fc399c77815eec3312359ae16daa5f3d5c3",
    "timestamp": 1652028409,
    "bits": 386495093,
    "nonce": 1198006405
  },
  "status": 2,
  "vbkrefs": [
    3186756
  ],
  "blockOfProofEndorsements": [
    "3d9134b1be1fa287b4aa9aff25bc40dd904ff624e16a8a2d752bb24a71e20e11"
  ]
}
 */
data class BtcBlockResponse(
    val chainWork: String,
    val height: Int,
    val header: BtcBlockHeader,
    val status: Int,
    val vbkrefs: List<Int>,
    val blockOfProofEndorsements: List<String>
)

data class BtcBlockHeader(
    val hash: String,
    val version: Int,
    val previousBlock: String,
    val merkleRoot: String,
    val timestamp: Int,
    val bits: Long,
    val nonce: Int
)

/*
{
  "chainWork": "00000000000000000000000000000000000000000000000000feab3d9d5b0000",
  "containingEndorsements": [
  ],
  "endorsedBy": [
  ],
  "height": 3321052,
  "header": {
    "hash": "0000000003f6ccf9299785573a3c24283b89e76eda71d95b",
    "height": 3321052,
    "version": 2,
    "previousBlock": "5ee9f0d20c1ebf6bc18dbcef",
    "previousKeystone": "abdd1cc4869a899df7",
    "secondPreviousKeystone": "99990eeb6edf9b48f9",
    "merkleRoot": "5db7e3221f903446a3406a8bf2150280",
    "timestamp": 1659867216,
    "difficulty": 85259378,
    "nonce": 481316250119,
    "id": "3a3c24283b89e76eda71d95b",
    "serialized": "410032acdc00025ee9f0d20c1ebf6bc18dbcefabdd1cc4869a899df799990eeb6edf9b48f95db7e3221f903446a3406a8bf215028062ef90500514f4727010af2207"
  },
  "status": 516,
  "altrefs": 1,
  "stored": {
    "vtbids": [
    ]
  },
  "blockOfProofEndorsements": [
  ]
}
 */

data class VbkBlockResponse(
    val chainWork: String,
    val containingEndorsements: List<String>,
    val endorsedBy: List<String>,
    val blockOfProofEndorsements: List<String>,
    val height: Int,
    val header: VbkHeader,
    val status: Int,
    val altrefs: Int,
    val stored: StoredInVbkBlockData,
)

data class VbkHeader (
    val height: Int,
    val version: Short,
    val previousBlock: PreviousBlockVbkHash,
    val previousKeystone: PreviousKeystoneVbkHash,
    val secondPreviousKeystone: PreviousKeystoneVbkHash,
    val merkleRoot: TruncatedMerkleRoot,
    val timestamp: Int,
    val difficulty: Int,
    val nonce: Long,
    val hash: VbkHash
)

data class StoredInVbkBlockData(
    val vtbids: List<String>
)
