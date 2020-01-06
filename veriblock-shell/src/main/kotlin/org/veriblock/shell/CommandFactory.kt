// VeriBlock Blockchain Project
// Copyright 2017-2018 VeriBlock, Inc
// Copyright 2018-2019 Xenios SEZC
// All rights reserved.
// https://www.veriblock.org
// Distributed under the MIT software license, see the accompanying
// file LICENSE or http://www.opensource.org/licenses/mit-license.php.

package org.veriblock.shell

import org.slf4j.LoggerFactory
import org.veriblock.core.utilities.AddressUtility
import org.veriblock.core.utilities.extensions.countChar
import org.veriblock.core.utilities.extensions.isHex
import org.veriblock.shell.core.ResultMessage
import java.math.BigDecimal
import kotlin.collections.set

private val logger = LoggerFactory.getLogger(CommandFactory::class.java)

const val FILENAME_SELECTOR = "-o"
const val FORMAT_SELECTOR = "-f"

class CommandResult(
    val command: Command,
    val parameters: Map<String, Any>
)

class ShellException(
    vararg val messages: ResultMessage = emptyArray()
) : RuntimeException()

class CommandFactory {
    private val commands: MutableMap<String, Command> = LinkedHashMap()

    fun getInstance(request: String?): CommandResult {
        logger.debug("User Command: {}", request)

        if (request.isNullOrEmpty()) {
            throw malformedCommandError()
        }

        val parts = request.split(" ".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()
        if (parts.isEmpty()) {
            throw malformedCommandError()
        }

        val form = parts[0]
        val command = commands[form]
            ?: throw unknownCommandError(form)

        val parameters = extractParameters(command, parts)

        return CommandResult(command, parameters)
    }

    private fun extractParameters(command: Command, suppliedParams: Array<String>): Map<String, Any> {
        if (suppliedParams.size - 1 > command.parameters.size) {
            throw syntaxError(command, "too many parameters provided")
        }

        return command.parameters.asSequence().mapIndexedNotNull { index, param ->
            val suppliedParamIndex = index + 1
            if (suppliedParamIndex < suppliedParams.size) {
                val suppliedParam = suppliedParams[suppliedParamIndex]
                param.name to extractTypedParam(param, suppliedParam, command)
            } else {
                if (param.required) {
                    throw syntaxError(
                        command,
                        "parameter '${param.name}' is required"
                    )
                }
                null
            }
        }.associate {
            it
        }
    }

    private fun extractTypedParam(param: CommandParameter, suppliedParam: String, command: Command): Any {
        return when (param.type) {

            CommandParameterType.STRING -> suppliedParam

            CommandParameterType.LONG -> suppliedParam.asPositiveLong()
                ?: throw syntaxError(
                    command,
                    "parameter '${param.name}' must be a positive 64-bit integer"
                )

            CommandParameterType.INTEGER -> suppliedParam.asPositiveInteger()
                ?: throw syntaxError(
                    command,
                    "parameter '${param.name}' must be a positive 32-bit integer"
                )

            CommandParameterType.AMOUNT -> try {
                BigDecimal(suppliedParam)
            } catch (e: NumberFormatException) {
                throw syntaxError(command, "parameter '${param.name}' must be an amount in BTC to send. e.g. 0.1")
            }

            CommandParameterType.HASH, CommandParameterType.HEXSTRING -> if (suppliedParam.isHex()) {
                suppliedParam
            } else {
                throw syntaxError(
                    command,
                    "parameter '${param.name}' must be a hexadecimal string"
                )
            }

            CommandParameterType.PEER -> if (suppliedParam.countChar(':') == 1) {
                suppliedParam
            } else {
                throw syntaxError(
                    command,
                    "parameter '${param.name}' must be a string in the form: host:port"
                )
            }

            CommandParameterType.STANDARD_ADDRESS -> if (AddressUtility.isValidStandardAddress(suppliedParam)) {
                suppliedParam
            } else {
                throw syntaxError(
                    command,
                    "parameter '${param.name}' must be a valid standard address"
                )
            }

            CommandParameterType.MULTISIG_ADDRESS -> if (AddressUtility.isValidMultisigAddress(suppliedParam)) {
                suppliedParam
            } else {
                throw syntaxError(
                    command,
                    "parameter '${param.name}' must be a valid standard address"
                )
            }

            CommandParameterType.STANDARD_OR_MULTISIG_ADDRESS -> if (AddressUtility.isValidStandardOrMultisigAddress(suppliedParam)) {
                suppliedParam
            } else {
                throw syntaxError(
                    command,
                    "parameter '${param.name}' must be a valid standard address"
                )
            }

            CommandParameterType.COMMA_SEPARATED_STANDARD_ADDRESSES -> {
                val addresses = suppliedParam.split(",")
                if (addresses.size !in 2..58) {
                    throw syntaxError(
                        command,
                        "parameter '${param.name}' must be comprised of between 2 and 58 standard addresses separated by commas!"
                    )
                }

                addresses.forEach {
                    throw syntaxError(
                        command,
                        "parameter '${param.name}' must be comprised of multiple standard addresses separated by commas, and '$it' is not a valid standard address"
                    )
                }

                suppliedParam
            }

            CommandParameterType.BOOLEAN -> {
               val lowerCase = suppliedParam.toLowerCase()
                if (lowerCase == "true" || lowerCase == "t") {
                    true
                } else if (lowerCase == "false" || lowerCase == "f") {
                    false
                } else {
                    throw syntaxError(
                        command,
                        "parameter '${param.name}' must be a boolean (true/false or t/f)"
                    )
                }
            }

            CommandParameterType.COMMA_SEPARATED_PUBLIC_KEYS_OR_ADDRESSES -> {
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

            CommandParameterType.COMMA_SEPARATED_SIGNATURES -> {
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
    }

    private fun malformedCommandError(): ShellException = ShellException(
        ResultMessage(
            "V005",
            "Malformed command string",
            "The passed command string cannot be parsed.",
            true
        )
    )

    private fun unknownCommandError(form: String) = ShellException(
        ResultMessage(
            "V004",
            "Unknown protocol command",
            "The command '$form' is not supported",
            true
        )
    )

    private fun syntaxError(command: Command, message: String) = ShellException(
        ResultMessage(
            "V009",
            "Syntax error",
            "Usage: ${command.form}${command.parameters.joinToString("") {
                if (it.required) " <${it.name}>" else " [${it.name}]"
            }} ERROR: $message",
            true)
    )

    fun getCommands(): Map<String, Command> = commands

    fun registerCommand(command: Command) {
        val aliases = command.form.split("|")
        for (alias in aliases) {
            commands[alias] = command
        }
    }
}

fun String.asPositiveLong() = toLongOrNull()?.let { if (it >= 0L) it else null }
fun String.asPositiveInteger() = toIntOrNull()?.let { if (it >= 0) it else null }
