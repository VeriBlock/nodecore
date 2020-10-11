
@file:JvmName("Debug")

package org.veriblock.core.debug

import org.veriblock.core.utilities.createLogger
import org.veriblock.core.wallet.AddressManager
import java.io.File
import java.io.PrintWriter
import java.io.StringWriter
import kotlin.system.measureTimeMillis

private val logger = createLogger {}

fun main() {
    logger.info("Installed packages (Ubuntu):")
    logger.info(getInstalledPackagesUbuntu())
    val manager = AddressManager().apply {
        load(File("wallet.dat"))
    }
    repeat(100_000) {
        val entropyBefore = getEntropy().replace("\"", "").trim().toInt()
        val timeTaken = measureTimeMillis { manager.getNewAddress() }
        val entropyAfter = getEntropy().replace("\"", "").trim().toInt()
        logger.info { "Generating address #$it took ${timeTaken}ms. Entropy before: $entropyBefore, entropy after: $entropyAfter" }
    }
}

fun getEntropy() = getCommandResult("cat /proc/sys/kernel/random/entropy_avail")

fun getInstalledPackagesUbuntu() = getCommandResult("dpkg -l")

fun getCommandResult(command: String): String = try {
    val cmd = arrayOf("/bin/sh", "-c", command)
    val p = Runtime.getRuntime().exec(cmd)
    p.inputStream.readBytes().decodeToString()
} catch (e: Exception) {
    val sw = StringWriter()
    val pw = PrintWriter(sw)
    e.printStackTrace(pw)
    pw.flush()
    val stackTrace = sw.toString()
    stackTrace.substring(0, (stackTrace.length - 1).coerceAtMost(500))
}
