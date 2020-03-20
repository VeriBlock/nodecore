// VeriBlock NodeCore CLI
// Copyright 2017-2020 Xenios SEZC
// All rights reserved.
// https://www.veriblock.org
// Distributed under the MIT software license, see the accompanying
// file LICENSE or http://www.opensource.org/licenses/mit-license.php.

package nodecore.cli.serialization;

import com.google.gson.annotations.SerializedName;
import nodecore.api.grpc.VeriBlockMessages;

public class GetDiagnosticInfoPayload {
    public GetDiagnosticInfoPayload(final VeriBlockMessages.GetDiagnosticInfoReply reply) {

        //Diagnostics
        workingDirectory = reply.getWorkingDirectory();
        userLanguage = reply.getUserLanguage();
        javaRuntimeName = reply.getJavaRuntimeName();
        sunBootLibraryPath = reply.getSunBootLibraryPath();
        javaRuntimeVersion = reply.getJavaRuntimeVersion();
        javaSpecificationVersion = reply.getJavaSpecificationVersion();
        osName = reply.getOsName();
        osArch = reply.getOsArch();
        osVersion = reply.getOsVersion();
        memoryTotalGB = reply.getMemoryTotalGb();
        memoryMaxGB = reply.getMemoryMaxGb();
        processorCount = reply.getProcessorCount();
        processorType = reply.getProcessorType();
        datetimeNowUtc = reply.getDatetimeNowUtc();

        //NC Properties
        nodecorePropertiesSource = reply.getNodecorePropertiesSource();

        int iCount = reply.getNodecorePropertiesValuesCount();
        nodecorePropertiesValues = new String[iCount];
        for (int i = 0; i < iCount; i++)
        {
            nodecorePropertiesValues[i] = reply.getNodecorePropertiesValues(i);
        }

        // Startup options
        nodecoreStartCommandline = reply.getNodecoreStartCommandline();

        // Environment Variables
        iCount = reply.getEnvironmentVariablesCount();
        environmentVariables = new String[iCount];
        for (int i = 0; i < iCount; i++) {
            environmentVariables[i] = reply.getEnvironmentVariables(i);
        }
    }

    @SerializedName("nodecore_working_directory")
    public String workingDirectory;

    @SerializedName("user_language")
    public String userLanguage;

    @SerializedName("java_runtime_name")
    public String javaRuntimeName;

    @SerializedName("sun_boot_library_path")
    public String sunBootLibraryPath;

    @SerializedName("java_runtime_version")
    public String javaRuntimeVersion;

    @SerializedName("java_specification_version")
    public String javaSpecificationVersion;

    @SerializedName("os_name")
    public String osName;

    @SerializedName("os_arch")
    public String osArch;

    @SerializedName("os_version")
    public String osVersion;

    @SerializedName("memory_total_gb")
    public String memoryTotalGB;

    @SerializedName("memory_max_gb")
    public String memoryMaxGB;

    @SerializedName("processor_count")
    public String processorCount;

    @SerializedName("processor_type")
    public String processorType;

    @SerializedName("datetime_now_utc")
    public String datetimeNowUtc;

    @SerializedName("nodecore_properties_source")
    public String nodecorePropertiesSource;

    @SerializedName("nodecore_properties_values")
    public String[] nodecorePropertiesValues;

    @SerializedName("nodecore_start_commandline")
    public String nodecoreStartCommandline;

    @SerializedName("environment_variables")
    public String[] environmentVariables;
}
