package nodecore.api.grpc.utilities.extensions

import com.google.protobuf.ByteString
import nodecore.api.grpc.utilities.ByteStringAddressUtility.createProperByteStringAutomatically
import nodecore.api.grpc.utilities.ByteStringAddressUtility.parseProperAddressTypeAutomatically

fun ByteString.toProperAddressType() = parseProperAddressTypeAutomatically(this)
fun String.asProperAddressTypeByteString() = createProperByteStringAutomatically(this)
