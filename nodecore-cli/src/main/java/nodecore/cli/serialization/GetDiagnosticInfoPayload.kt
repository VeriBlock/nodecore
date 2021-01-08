// VeriBlock NodeCore CLI
// Copyright 2017-2021 Xenios SEZC
// All rights reserved.
// https://www.veriblock.org
// Distributed under the MIT software license, see the accompanying
// file LICENSE or http://www.opensource.org/licenses/mit-license.php.
package nodecore.cli.serialization

import com.google.gson.annotations.SerializedName
import nodecore.api.grpc.VeriBlockMessages.GetDiagnosticInfoReply

class GetDiagnosticInfoPayload(reply: GetDiagnosticInfoReply) {
    @SerializedName("nodecore_working_directory")
    val workingDirectory = reply.workingDirectory

    @SerializedName("user_language")
    val userLanguage = reply.userLanguage

    @SerializedName("java_runtime_name")
    val javaRuntimeName = reply.javaRuntimeName

    @SerializedName("sun_boot_library_path")
    val sunBootLibraryPath = reply.sunBootLibraryPath

    @SerializedName("java_runtime_version")
    val javaRuntimeVersion = reply.javaRuntimeVersion

    @SerializedName("java_specification_version")
    val javaSpecificationVersion = reply.javaSpecificationVersion

    @SerializedName("os_name")
    val osName = reply.osName

    @SerializedName("os_arch")
    val osArch = reply.osArch

    @SerializedName("os_version")
    val osVersion = reply.osVersion

    @SerializedName("memory_total_gb")
    val memoryTotalGB = reply.memoryTotalGb

    @SerializedName("memory_max_gb")
    val memoryMaxGB = reply.memoryMaxGb

    @SerializedName("processor_count")
    val processorCount = reply.processorCount

    @SerializedName("processor_type")
    val processorType = reply.processorType

    @SerializedName("datetime_now_utc")
    val datetimeNowUtc = reply.datetimeNowUtc

    @SerializedName("nodecore_properties_source")
    val nodecorePropertiesSource = reply.nodecorePropertiesSource

    @SerializedName("nodecore_properties_values")
    val nodecorePropertiesValues = Array(reply.nodecorePropertiesValuesCount) { index ->
        reply.getNodecorePropertiesValues(index)
    }

    @SerializedName("nodecore_start_commandline")
    val nodecoreStartCommandline = reply.nodecoreStartCommandline

    @SerializedName("environment_variables")
    val environmentVariables = Array(reply.environmentVariablesCount) { index ->
        reply.getEnvironmentVariables(index)
    }
}
