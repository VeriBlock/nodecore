// VeriBlock Blockchain Project
// Copyright 2017-2018 VeriBlock, Inc
// Copyright 2018-2021 Xenios SEZC
// All rights reserved.
// https://www.veriblock.org
// Distributed under the MIT software license, see the accompanying
// file LICENSE or http://www.opensource.org/licenses/mit-license.php.
package bootstrap.downloader

import com.google.gson.Gson
import io.ktor.client.HttpClient
import io.ktor.client.engine.cio.CIO
import io.ktor.client.features.json.Json
import io.ktor.client.request.get
import io.ktor.client.statement.HttpResponse
import io.ktor.utils.io.jvm.javaio.copyTo
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import me.tongfei.progressbar.DelegatingProgressBarConsumer
import me.tongfei.progressbar.ProgressBarBuilder
import me.tongfei.progressbar.ProgressBarStyle
import mu.KotlinLogging
import org.apache.commons.codec.digest.DigestUtils
import org.veriblock.core.SharedConstants
import org.veriblock.core.utilities.Configuration
import org.veriblock.core.utilities.bootOption
import org.veriblock.core.utilities.bootOptions
import java.io.File
import java.io.IOException
import java.lang.IllegalStateException
import java.nio.file.Files
import java.nio.file.Paths
import java.nio.file.StandardCopyOption
import java.util.*

private val logger = createLogger {}

class Downloader(
    private val url: String,
    private val network: String,
    private val dataDirectory: String,
    private val isLocalUrl: Boolean = false
) {
    private val httpClient = HttpClient(CIO) {
        Json {}
    }

    private suspend fun downloadBlockList() = if (!isLocalUrl) {
        httpClient.get<BlockList>("$url/$network/blockFiles.json")
    } else {
        val localFile = File("$url/$network/blockFiles.json")
        if (localFile.exists()) {
            localFile.readText().toBlockList()
        } else {
            error("Unable to find the local file: ${localFile.toPath()}")
        }
    }

    private fun String.toBlockList() = Gson().fromJson(this, BlockList::class.java)

    private fun checkFilesToDownload(blockFiles: BlockList): List<BlockFile> {
        logger.info { "Checking your existing block files from ${Paths.get(dataDirectory, network)}..." }

        // Check how many files we should download
        return ProgressBarBuilder().apply {
            setTaskName("Checking your block files")
            setInitialMax(blockFiles.files.size.toLong())
            setStyle(ProgressBarStyle.ASCII)
            setConsumer(DelegatingProgressBarConsumer(logger::info, 100))
        }.build().use { progressBar ->
            blockFiles.files.mapNotNull { file ->
                // Verify the local file size and checksum
                val localFile = Paths.get(dataDirectory, network, file.folder, file.name).toFile()
                val toDownload = try {
                    if (localFile.isMissingOrCorrupted(file.size, file.checksum)) {
                        logger.debug { "Added ${file.name} to be downloaded" }
                        file
                    } else {
                        null
                    }
                } catch(e: Exception) {
                    logger.debug { "Unable to check the local file ${localFile.name}: ${e.message}" }
                    null
                }
                // Increase the progress bar
                progressBar.stepBy(1L)
                toDownload
            }
        }
    }

    private suspend fun downloadFiles(files: List<BlockFile>) {
        val filesToDownload = files.toMutableList()

        // Download the files
        logger.info { "Downloading ${filesToDownload.size} block files to ${Paths.get(dataDirectory, network)}..." }

        var failedAttempts = 0

        ProgressBarBuilder().apply {
            setTaskName("Downloading block files")
            setInitialMax(filesToDownload.size.toLong())
            setStyle(ProgressBarStyle.ASCII)
            setConsumer(DelegatingProgressBarConsumer(logger::info, 100))
        }.build().use { progressBar ->
            while (filesToDownload.isNotEmpty()) {
                val file = filesToDownload.removeAt(0)
                val localFile = Paths.get(dataDirectory, network, file.folder, file.name).toFile()
                if (!localFile.parentFile.exists()) {
                    localFile.parentFile.mkdirs()
                }

                try {
                    logger.debug { "Downloading ${file.name}..." }
                    // We should delete the nodecore.dat cache files when nodecore.dat should be downloaded
                    if (file.name == "nodecore.dat") {
                        Paths.get(dataDirectory, network, file.folder).toFile().listFiles()?.filter {
                            it.name == "nodecore.dat-shm" || it.name == "nodecore.dat-wal"
                        }?.forEach {
                            logger.debug { "Deleting ${it.name}" }
                            it.delete()
                        }
                    }

                    // Download the file
                    if (!isLocalUrl) {
                        httpClient.get<HttpResponse>(
                            "$url/$network/${file.folder}/${file.name}"
                        ).content.copyTo(
                            Paths.get(dataDirectory, network, file.folder, file.name).toFile().outputStream()
                        )
                    } else {
                        File("$url/$network/${file.folder}/${file.name}").inputStream().use { inputStream ->
                            Files.copy(
                                inputStream,
                                Paths.get(dataDirectory, network, file.folder, file.name),
                                StandardCopyOption.REPLACE_EXISTING
                            )
                        }
                    }

                    // Verify the integrity for the recently downloaded file
                    if (localFile.isMissingOrCorrupted(file.size, file.checksum)) {
                        logger.info { "The recently downloaded file ${file.folder}/${file.name} seems to be corrupted, the file has been re added to the download queue" }
                        filesToDownload.add(file)
                    } else {
                        // Increase the progress bar
                        progressBar.stepBy(1L)
                        failedAttempts = 0
                    }
                } catch (e: IOException) {
                    logger.info { "Failed to download ${file.name}: ${e.message}" }
                    failedAttempts++
                    if (failedAttempts < 10) {
                        logger.info { "The file has been re added to the download queue." }
                        filesToDownload.add(file)
                    } else {
                        throw IllegalStateException("Failed too many times to download files")
                    }
                }
            }
        }
    }

    suspend fun downloadBlocks() {
        var finished = false
        while (!finished) {
            try {
                logger.info { "Checking the $network bootstrap files from $url/$network..." }
                val blockList = downloadBlockList()
                logger.info { "Detected ${blockList.files.size} block files, last updated at ${blockList.updateDate}..." }
                val filesToDownload = checkFilesToDownload(blockList)
                downloadFiles(filesToDownload)
                finished = true
            } catch (e: Exception) {
                // Mostly the remote server is inaccessible
                logger.debug(e) { "Failed to download the files" }
                logger.info { "Failed to access the remote host. This might mean that the remote host is receiving an scheduled daily update" }
                logger.info { "The download progress will be paused for 30 minutes and then it will be restarted..." }
                delay(30 * 60 * 1000)
            }
        }
    }
}

fun createLogger(context: () -> Unit) = KotlinLogging.logger(context)

fun File.isMissingOrCorrupted(size: Long, checksum: String): Boolean {
    val fileExist = exists()
    if (!fileExist) {
        logger.debug { "The file ${this.name} doesn't exist" }
        return true
    }
    var fileLength: Long
    var fileChecksum: String
    val isCorrupted = inputStream().use { inputStream ->
        fileLength = length()
        fileChecksum = DigestUtils.md5Hex(inputStream)
        fileLength != size || fileChecksum != checksum
    }
    val isMissingOrCorrupted = !fileExist || isCorrupted
    if (isMissingOrCorrupted) {
        logger.debug { "File: ${this.name}, exists: $fileExist, fileLength: $fileLength (expected length: $size), fileChecksum: $fileChecksum (expected checksum: $checksum): isCorrupted: $isCorrupted, isMissingOrCorrupted: $isMissingOrCorrupted" }
    }
    return isMissingOrCorrupted
}

data class BlockList(
    val updateDate: String,
    val files: List<BlockFile>
)

data class BlockFile(
    val name: String,
    val folder: String,
    val size: Long,
    val checksum: String
)

data class DownloaderConfig(
    val network: String = "mainnet",
    val url: String = "https://mirror.veriblock.org/bootstrap",
    val dataDir: String = getDefaultNodecoreDataDir(),
    val localUrl: Boolean = false,
    val displayHelp: Boolean = false
)

fun getDefaultNodecoreDataDir(): String {
    // Get the root folder from the package
    val packageParentFolder = Paths.get("").toAbsolutePath().parent?.parent
        ?: return "./"
    logger.info {"Package root folder: $packageParentFolder"}

    // Search the NodeCore folder inside the package
    val nodeCoreFolder = packageParentFolder.toFile().listFiles()?.find {
        it.name.startsWith("nodecore-0") // nodecore-0.x.x...
    } ?: return "./"

    logger.info { "Found the NodeCore package: ${nodeCoreFolder.absolutePath}" }
    return Paths.get(nodeCoreFolder.toPath().toString(), "bin").toString()
}

suspend fun main(args: Array<String>) {
    print(SharedConstants.LICENSE)
    println(SharedConstants.VERIBLOCK_APPLICATION_NAME.replace("$1", ApplicationMeta.FULL_APPLICATION_NAME_VERSION))
    println("\t\t${SharedConstants.VERIBLOCK_WEBSITE}")
    println("\t\t${SharedConstants.VERIBLOCK_EXPLORER}\n")
    println("${SharedConstants.VERIBLOCK_PRODUCT_WIKI_URL.replace("$1", "https://wiki.veriblock.org/index.php/Bootstrap_Downloader")}\n")

    val options = listOf(
        bootOption(
            opt = "n",
            longOpt = "network",
            desc = "Specify the target network (testnet / mainnet)",
            argName = "network",
            configMapping = "downloader.network"
        ),
        bootOption(
            opt = "u",
            longOpt = "url",
            desc = "Specify the download url",
            argName = "url",
            configMapping = "downloader.url"
        ),
        bootOption(
            opt = "d",
            longOpt = "dataDir",
            desc = "Specify the data directory where NodeCore generated files reside",
            argName = "dataDir",
            configMapping = "downloader.dataDir"
        ),
        bootOption(
            opt = "l",
            longOpt = "localUrl",
            desc = "Specify if the download url is a local url (true / false)",
            argName = "localUrl",
            configMapping = "downloader.localUrl"
        ),
        bootOption(
            opt = "h",
            desc = "Display all the program arguments",
            configMapping = "downloader.displayHelp"
        )
    )

    val bootOptions = try {
        bootOptions(options, args)
    } catch (e: Exception) {
        logger.error { "Unable to parse the program arguments: ${e.message}, use the -h argument to display the available arguments" }
        return
    }

    val config = Configuration(
        bootOptions = bootOptions
    ).extract<DownloaderConfig>("downloader") ?: DownloaderConfig()

    if (config.displayHelp) {
        logger.info { "Available program arguments:" }
        options.forEach {
            logger.info { "-${it.opt}: ${it.desc}" }
        }
        return
    }

    val networkDirectory = Paths.get(config.dataDir, config.network).toFile()
    try {
        if (networkDirectory.exists() && !networkDirectory.canWrite()) {
            logger.info { "Unable to write at the $networkDirectory directory" }
            return
        }
    } catch (e: SecurityException) {
        logger.info { "Unable to write at the $networkDirectory directory: ${e.message}" }
        return
    }
    Downloader(
        config.url,
        config.network,
        config.dataDir,
        config.localUrl
    ).downloadBlocks()

    logger.info { "Finished! Press any key to exit..." }
    Scanner(System.`in`).nextLine()
}
