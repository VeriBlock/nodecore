package nodecore.api.grpc.utilities.extensions

import com.google.protobuf.ByteString
import org.veriblock.core.utilities.Utility
import org.veriblock.core.utilities.extensions.asBase58Bytes
import org.veriblock.core.utilities.extensions.asBase59Bytes
import org.veriblock.core.utilities.extensions.asBase64Bytes
import org.veriblock.core.utilities.extensions.asHexBytes
import org.veriblock.core.utilities.extensions.toBase58
import org.veriblock.core.utilities.extensions.toBase59
import org.veriblock.core.utilities.extensions.toBase64
import org.veriblock.core.utilities.extensions.toHex

fun String.asBase58ByteString(): ByteString =
    ByteString.copyFrom(asBase58Bytes())

fun String.asBase59ByteString(): ByteString =
    ByteString.copyFrom(asBase59Bytes())

fun ByteString.toBase58(): String =
    toByteArray().toBase58()

fun ByteString.toBase59(): String =
    toByteArray().toBase59()

fun String.asBase64ByteString(): ByteString =
    ByteString.copyFrom(asBase64Bytes())

fun ByteString.toBase64(): String =
    toByteArray().toBase64()

fun String.asHexByteString(): ByteString =
    ByteString.copyFrom(asHexBytes())

fun ByteString.toHex(): String =
    toByteArray().toHex()

fun ByteArray.toByteString(): ByteString =
    ByteString.copyFrom(this)
