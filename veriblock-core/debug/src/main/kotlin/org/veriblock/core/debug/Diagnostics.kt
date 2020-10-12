package org.veriblock.core.debug

import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json
import org.veriblock.core.utilities.debugWarn
import java.text.SimpleDateFormat
import java.util.Date

private val json = Json { prettyPrint = true }

fun printDiagnostics() {
    // Get the default diagnostic information
    printDiagnosticInfo()

    logger.info("Installed packages (Ubuntu):")
    logger.info(getCommandResult("dpkg -l"))
}

fun printDiagnosticInfo() {
    try {
        val diagnosticInfo = DiagnosticInfo(
            //Runtime.getRuntime().availableProcessors();
            user_language = System.getProperty("user.language"),
            java_runtime_name = System.getProperty("java.runtime.name"),
            sun_boot_library_path = System.getProperty("sun.boot.library.path"),
            java_runtime_version = System.getProperty("java.runtime.version"),
            working_directory = System.getProperty("user.dir"),
            java_specification_version = System.getProperty("java.specification.version"),
            os_name = System.getProperty("os.name"),
            os_arch = System.getProperty("os.arch"),
            os_version = System.getProperty("os.version"),
            memory_total_gb = String.format("%.2f", Runtime.getRuntime().totalMemory().toDouble() / (1024 * 1024 * 1024)) + " GB",
            memory_max_gb = String.format("%.2f", Runtime.getRuntime().maxMemory().toDouble() / (1024 * 1024 * 1024)) + " GB",
            processor_count = Integer.toString(Runtime.getRuntime().availableProcessors()),
            processor_type = System.getenv()["PROCESSOR_IDENTIFIER"],

            //capture global UTC. Logs should also be UTC, but this is one-single place, and allows easy verification. Checks-and-balances.
            datetime_now_utc = SimpleDateFormat("YYYY-MM-dd HH:mm:ss.SSSXX").format(Date())
        )
        logger.info { json.encodeToString(diagnosticInfo) }
    } catch (e: Exception) {
        //don't crash on diagnostics info
        logger.debugWarn(e) { "Unable to retrieve diagnostic info" }
    }
}

@Serializable
class DiagnosticInfo(
    val user_language: String,
    val java_runtime_name: String,
    val sun_boot_library_path: String,
    val java_runtime_version: String,
    val java_specification_version: String,
    val os_name: String,
    val os_arch: String,
    val os_version: String,
    val memory_total_gb: String,
    val memory_max_gb: String,
    val processor_count: String,
    val processor_type: String?,
    val working_directory: String,
    val datetime_now_utc: String
)
