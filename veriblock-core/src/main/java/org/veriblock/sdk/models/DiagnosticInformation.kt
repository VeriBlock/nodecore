package org.veriblock.sdk.models

import org.veriblock.core.utilities.DiagnosticInfo

data class DiagnosticInformation(
    val information: List<String>,
    val diagnosticInfo: DiagnosticInfo
)
