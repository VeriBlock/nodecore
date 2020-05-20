package org.veriblock.spv.standalone.commands

import org.veriblock.core.utilities.AddressUtility
import org.veriblock.shell.CommandParameterMapper
import org.veriblock.shell.syntaxError

object ShellCommandParameterMappers {

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
}
