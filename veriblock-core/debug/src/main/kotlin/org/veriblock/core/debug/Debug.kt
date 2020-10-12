
@file:JvmName("Debug")

package org.veriblock.core.debug

import io.ktor.client.HttpClient
import io.ktor.client.engine.cio.CIO
import io.ktor.client.request.get
import io.ktor.client.statement.HttpResponse
import io.ktor.utils.io.jvm.javaio.copyTo
import org.veriblock.core.utilities.createLogger
import org.veriblock.core.wallet.AddressManager
import java.io.File
import java.io.PrintWriter
import java.io.StringWriter
import java.nio.file.Paths
import kotlin.system.exitProcess
import kotlin.system.measureTimeMillis

val logger = createLogger {}

private val httpClient = HttpClient(CIO)

suspend fun main(args: Array<String>) {
    if (args.isEmpty()) {
        logger.error { "The program requires to be passed a wallet file URL as an argument" }
        exitProcess(1)
    }

    val fileUrl = args[0]

    printDiagnostics()

    val downloadedWalletFile = File("downloaded_wallet.dat")
    downloadedWalletFile.delete()
    try {
        httpClient.get<HttpResponse>(fileUrl).content.copyTo(
            downloadedWalletFile.outputStream()
        )
    } catch (e: Exception) {
        logger.error { "Unable to download wallet file $fileUrl" }
        exitProcess(1)
    }

    logger.info { "Testing tx creation on default wallet..." }
    testWalletFile(File("wallet.dat"))

    logger.info { "Testing tx creation on downloaded wallet..." }
    testWalletFile(downloadedWalletFile)

    logger.info { "Tests finished! You can find the logs at ${File(".").canonicalPath}/debug.log" }
}

private fun testWalletFile(file: File) {
    val manager = AddressManager().apply {
        load(file)
    }
    repeat(10) {
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
