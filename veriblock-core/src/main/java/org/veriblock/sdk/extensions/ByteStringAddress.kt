package org.veriblock.sdk.extensions

import com.google.protobuf.ByteString
import org.veriblock.sdk.extensions.ByteStringAddressUtility.createProperByteStringAutomatically
import org.veriblock.sdk.extensions.ByteStringAddressUtility.parseProperAddressTypeAutomatically

fun ByteString.toProperAddressType() = parseProperAddressTypeAutomatically(this)
fun String.asProperAddressTypeByteString() = createProperByteStringAutomatically(this)
