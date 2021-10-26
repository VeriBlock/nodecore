// VeriBlock NodeCore
// Copyright 2017-2021 Xenios SEZC
// All rights reserved.
// https://www.veriblock.org
// Distributed under the MIT software license, see the accompanying
// file LICENSE or http://www.opensource.org/licenses/mit-license.php.
package org.veriblock.core.utilities

import com.sun.management.OperatingSystemMXBean
import java.lang.management.ManagementFactory
import java.nio.file.FileSystem
import java.nio.file.FileSystems
import java.nio.file.Files
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import org.veriblock.core.types.SimpleResult

fun getDiagnosticInfo(): DiagnosticInfo {
    return try {
        val osBean: OperatingSystemMXBean? = try {
            ManagementFactory.getPlatformMXBean(OperatingSystemMXBean::class.java)
        } catch (e: Exception) {
            null
        }
        // Compute sizes, making sure the call doesn't fail
        val (freeSpace, totalSpace) = try {
            FileSystems.getDefault().rootDirectories.fold(0L to 0L) { sizes, path ->
                try {
                    val fs = Files.getFileStore(path)
                    (sizes.first + fs.usableSpace) to (sizes.second + fs.totalSpace)
                } catch (e: Exception) {
                    sizes
                }
            }
        } catch (e: Exception) {
            null to null
        }
        val gigabyte = 1024 * 1024 * 1024L
        DiagnosticInfo(
            user_language = getSystemPropertyOrUndefined("user.language"),
            java_runtime_name = getSystemPropertyOrUndefined("java.runtime.name"),
            sun_boot_library_path = getSystemPropertyOrUndefined("sun.boot.library.path"),
            java_runtime_version = getSystemPropertyOrUndefined("java.runtime.version"),
            working_directory = getSystemPropertyOrUndefined("user.dir"),
            java_specification_version = getSystemPropertyOrUndefined("java.specification.version"),
            os_name = getSystemPropertyOrUndefined("os.name"),
            os_arch = getSystemPropertyOrUndefined("os.arch"),
            os_version = getSystemPropertyOrUndefined("os.version"),
            memory_total_gb = String.format("%.2f", Runtime.getRuntime().totalMemory().toDouble() / (1024 * 1024 * 1024)) + " GB",
            memory_max_gb = String.format("%.2f", Runtime.getRuntime().maxMemory().toDouble() / (1024 * 1024 * 1024)) + " GB",
            processor_count = Runtime.getRuntime().availableProcessors().toString(),
            processor_type = System.getenv()["PROCESSOR_IDENTIFIER"] ?: "Undefined",
            process_cpu_load = osBean?.let { "${it.processCpuLoad * 100}%" } ?: "Undefined",
            system_cpu_load = osBean?.let { "${it.systemCpuLoad * 100}%" } ?: "Undefined",
            disk_free_space = freeSpace?.let { String.format("%.03fGB", it.toDouble() / gigabyte) } ?: "Undefined",
            disk_total_space = totalSpace?.let { String.format("%.03fGB", it.toDouble() / gigabyte) } ?: "Undefined",
            datetime_now_utc = SimpleDateFormat("YYYY-MM-dd HH:mm:ss.SSSXX").format(Date())
        )
    } catch(exception: Exception) {
        //don't crash on diagnostics info
        DiagnosticInfo()
    }
}

fun checkJvmVersion(): SimpleResult {
    val rawVersionString = System.getProperty("java.specification.version").lowercase(Locale.getDefault())
    val versionString = if (rawVersionString.contains(".")) {
        rawVersionString.substring(rawVersionString.indexOf(".") + 1)
    } else {
        rawVersionString
    }
    val jvmEnvironmentError = StringBuilder()
    val arch = System.getProperty("os.arch")
    val version = versionString.toInt()
    val wrongVersion = if (version < 8 || version > 14 || (arch != "amd64" && arch != "x86_64" && arch != "aarch64")) {
        jvmEnvironmentError.appendLine("ERROR: The NodeCore Suite only supports Java 8, 9, 10, 11, 12, 13 and 14 (64-bit)")
        jvmEnvironmentError.appendLine("Current installed version $version ($arch) is not supported!")
        jvmEnvironmentError.appendLine("It is recommended to use Java 14")
        jvmEnvironmentError.appendLine("In order to continue, please download 64-bit Java 14!")
        jvmEnvironmentError.appendLine("Please see https://wiki.veriblock.org/index.php?title=NodeCore_Operations#Software for more details.")
        true
    } else {
        false
    }
    return SimpleResult(!wrongVersion, jvmEnvironmentError.toString())
}

fun getSystemPropertyOrUndefined(property: String): String = System.getProperty(property, "Undefined")

data class DiagnosticInfo(
    val user_language: String = "Undefined",
    val java_runtime_name: String = "Undefined",
    val sun_boot_library_path: String = "Undefined",
    val java_runtime_version: String = "Undefined",
    val java_specification_version: String = "Undefined",
    val os_name: String = "Undefined",
    val os_arch: String = "Undefined",
    val os_version: String = "Undefined",
    val memory_total_gb: String = "Undefined",
    val memory_max_gb: String = "Undefined",
    val processor_count: String = "Undefined",
    val processor_type: String = "Undefined",
    val process_cpu_load: String = "Undefined",
    val system_cpu_load: String = "Undefined",
    val disk_free_space: String = "Undefined",
    val disk_total_space: String = "Undefined",
    val working_directory: String = "Undefined",
    val datetime_now_utc: String = "Undefined"
)
