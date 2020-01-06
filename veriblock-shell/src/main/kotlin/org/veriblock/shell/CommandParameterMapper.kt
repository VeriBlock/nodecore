package org.veriblock.shell

import org.veriblock.core.utilities.AddressUtility
import org.veriblock.core.utilities.extensions.countChar
import org.veriblock.core.utilities.extensions.isHex
import java.math.BigDecimal

class CommandWithParam(
    val command: Command,
    val param: CommandParameter
)

typealias CommandParameterMapper = CommandWithParam.(String) -> Any

object CommandParameterMappers {
    val STRING: CommandParameterMapper = { suppliedParam ->
        suppliedParam
    }

    val LONG: CommandParameterMapper = { suppliedParam ->
        suppliedParam.asPositiveLong()
            ?: throw syntaxError(
                command,
                "parameter '${param.name}' must be a positive 64-bit integer"
            )
    }

    val INTEGER: CommandParameterMapper = { suppliedParam ->
        suppliedParam.asPositiveInteger()
            ?: throw syntaxError(
                command,
                "parameter '${param.name}' must be a positive 32-bit integer"
            )
    }

    val BOOLEAN: CommandParameterMapper = { suppliedParam ->
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

    val AMOUNT: CommandParameterMapper = { suppliedParam ->
        try {
            BigDecimal(suppliedParam)
        } catch (e: NumberFormatException) {
            throw syntaxError(command, "parameter '${param.name}' must be an amount in BTC to send. e.g. 0.1")
        }
    }

    val HEX_STRING: CommandParameterMapper = { suppliedParam ->
        if (suppliedParam.isHex()) {
            suppliedParam
        } else {
            throw syntaxError(
                command,
                "parameter '${param.name}' must be a hexadecimal string"
            )
        }
    }
}
