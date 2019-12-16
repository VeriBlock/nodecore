package nodecore.api.grpc.utilities.extensions

import com.google.protobuf.ByteString
import org.veriblock.core.utilities.Utility


fun String.asBase58ByteString(): ByteString? {
    return ByteString.copyFrom(Utility.base58ToBytes(this))
}

fun base59ToByteString(value: String?): ByteString? {
    return ByteString.copyFrom(Utility.base59ToBytes(value))
}

fun byteStringToBase58(value: ByteString): String? {
    return Utility.bytesToBase58(value.toByteArray())
}

fun byteStringToBase59(value: ByteString): String? {
    return Utility.bytesToBase59(value.toByteArray())
}

fun base64ToByteString(value: String?): ByteString? {
    return ByteString.copyFrom(Utility.base64ToBytes(value))
}

fun byteStringToBase64(value: ByteString): String? {
    return Utility.bytesToBase64(value.toByteArray())
}

fun hexToByteString(value: String?): ByteString? {
    return ByteString.copyFrom(Utility.hexToBytes(value))
}

fun bytesToByteString(value: ByteArray?): ByteString? {
    return ByteString.copyFrom(value)
}

fun byteStringToHex(value: ByteString): String? {
    return Utility.bytesToHex(value.toByteArray())
}
