package nodecore.cli.commands

import nodecore.cli.contracts.EndpointTransportType
import nodecore.cli.contracts.PeerEndpoint
import org.veriblock.core.params.getDefaultNetworkParameters
import org.veriblock.core.utilities.AddressUtility
import org.veriblock.core.utilities.extensions.isHex
import org.veriblock.shell.CommandParameterMapper
import org.veriblock.shell.CommandParameterMappers
import org.veriblock.shell.syntaxError
import java.util.*

object ShellCommandParameterMappers {
    val HASH: CommandParameterMapper = CommandParameterMappers.HEX_STRING

    val PEER: CommandParameterMapper = { suppliedParam ->
        try {
            val transportType: EndpointTransportType = when {
                suppliedParam.lowercase(Locale.getDefault()).startsWith("http://") -> EndpointTransportType.HTTP
                suppliedParam.lowercase(Locale.getDefault()).startsWith("https://") -> EndpointTransportType.HTTPS
                else -> EndpointTransportType.HTTP
            }
            val parsedAddress = suppliedParam.replace("https://", "").replace("http://", "")
            val parts = parsedAddress.split(":").toTypedArray()
            PeerEndpoint(
                address = parts[0],
                port = parts[1].toShort(),
                transportType = transportType
            )
        } catch(ignored: Exception) {
            throw syntaxError(
                command,
                "parameter '${param.name}' must be a string in the form: host:port"
            )
        }
    }

    var NET: CommandParameterMapper = { suppliedParam ->
        try {
            getDefaultNetworkParameters(suppliedParam)
        } catch(ignored: Exception) {
            throw syntaxError(
                    command,
                    "parameter '${param.name}' must be a string in the form: mainnet / testnet / testnet_progpow / alphanet / regtest_progpow"
            )
        }
    }

    val STANDARD_ADDRESS: CommandParameterMapper = { suppliedParam ->
        if (AddressUtility.isValidStandardAddress(suppliedParam)) {
            suppliedParam
        } else {
            throw syntaxError(
                command,
                "parameter '${param.name}' must be a valid standard address"
            )
        }
    }

    val MULTISIG_ADDRESS: CommandParameterMapper = { suppliedParam ->
        if (AddressUtility.isValidMultisigAddress(suppliedParam)) {
            suppliedParam
        } else {
            throw syntaxError(
                command,
                "parameter '${param.name}' must be a valid standard address"
            )
        }
    }

    val STANDARD_OR_MULTISIG_ADDRESS: CommandParameterMapper = { suppliedParam ->
        if (AddressUtility.isValidStandardOrMultisigAddress(suppliedParam)) {
            suppliedParam
        } else {
            throw syntaxError(
                command,
                "parameter '${param.name}' must be a valid standard address"
            )
        }
    }

    val COMMA_SEPARATED_STANDARD_ADDRESSES: CommandParameterMapper = { suppliedParam ->
        val addresses = suppliedParam.split(",")
        if (addresses.size !in 2..58) {
            throw syntaxError(
                command,
                "parameter '${param.name}' must be comprised of between 2 and 58 standard addresses separated by commas!"
            )
        }
        addresses.forEach {
            if (!AddressUtility.isValidStandardAddress(it)) {
                throw syntaxError(
                    command,
                    "parameter '${param.name}' must be comprised of multiple standard addresses separated by commas, and '$it' is not a valid standard address"
                )
            }
        }
        addresses
    }

    val COMMA_SEPARATED_PUBLIC_KEYS_OR_ADDRESSES: CommandParameterMapper = { suppliedParam ->
        val publicKeysOrAddresses = suppliedParam.split(",")
        if (publicKeysOrAddresses.size !in 2..58) {
            throw syntaxError(
                command,
                "parameter '${param.name}' must be comprised of between 2 and 58 addresses or hex-encoded public keys separated by commas!"
            )
        }

        publicKeysOrAddresses.forEach {
            if (!AddressUtility.isValidStandardAddress(it)) {
                if (!it.isHex()) {
                    throw syntaxError(
                        command,
                        "parameter '${param.name}' must be comprised of multiple hex-encoded public keys or addresses separated by commas, and '$it' is not a valid hex-encoded public key or standard address!"
                    )
                }

                if (it.isNotEmpty() && it.length != 176) {
                    throw syntaxError(
                        command,
                        "parameter '${param.name}' must be comprised of multiple hex-encoded public keys or addresses separated by commas, and '$it' is not a valid hex-encoded public key or standard address (should be 88 bytes, or valid standard address)!"
                    )
                }
            }
        }

        suppliedParam
    }

    val COMMA_SEPARATED_SIGNATURES: CommandParameterMapper = { suppliedParam ->
        val signatures = suppliedParam.split(",")
        if (signatures.size !in 2..58) {
            throw syntaxError(
                command,
                "parameter '${param.name}' must be comprised of between 2 and 58 hex-encoded public keys separated by commas!"
            )
        }

        signatures.forEach {
            if (!it.isHex()) {
                throw syntaxError(
                    command,
                    "parameter '${param.name}' must be comprised of multiple hex-encoded signatures separated by commas, and '$it' is not a valid hex-encoded signature!"
                )
            }
            if (it.isNotEmpty() && (it.length < 120 || it.length > 144)) {
                throw syntaxError(
                    command,
                    "parameter '${param.name}' must be comprised of multiple hex-encoded public keys addresses separated by commas, and '$it' is not a valid hex-encoded public key (should be between 60 and 72 bytes, unless blank)!"
                )
            }
        }
        suppliedParam
    }
}
