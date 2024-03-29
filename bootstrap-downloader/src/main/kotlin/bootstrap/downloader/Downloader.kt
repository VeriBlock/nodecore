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
import io.ktor.client.call.receive
import io.ktor.client.engine.cio.CIO
import io.ktor.client.features.json.Json
import io.ktor.client.request.get
import io.ktor.client.statement.HttpStatement
import io.ktor.utils.io.ByteReadChannel
import io.ktor.utils.io.core.isEmpty
import io.ktor.utils.io.core.readBytes
import kotlinx.coroutines.delay
import me.tongfei.progressbar.DelegatingProgressBarConsumer
import me.tongfei.progressbar.ProgressBarBuilder
import me.tongfei.progressbar.ProgressBarStyle
import mu.KotlinLogging
import org.apache.commons.codec.digest.DigestUtils
import org.veriblock.core.SharedConstants
import org.veriblock.core.utilities.Configuration
import java.io.File
import java.io.IOException
import java.lang.IllegalStateException
import java.nio.file.Files
import java.nio.file.Paths
import java.nio.file.StandardCopyOption
import java.util.*
import org.veriblock.core.utilities.checkJvmVersion
import kotlin.math.roundToInt

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
            setInitialMax(filesToDownload.sumOf { it.size })
            setUnit("MB", 1024 * 1024)
            setStyle(ProgressBarStyle.ASCII)
            setConsumer(DelegatingProgressBarConsumer(logger::info, 100))
        }.build().use { progressBar ->
            var cumulativeSize = 0L
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

                    // Temp file
                    val uuid = UUID.randomUUID()
                    val tempFile = Paths.get(dataDirectory, network, file.folder, "${file.name}-$uuid.dat").toFile()

                    // Download the file
                    if (!isLocalUrl) {
                        httpClient.get<HttpStatement>(
                            "$url/$network/${file.folder}/${file.name}"
                        ).execute { httpResponse ->
                            val channel: ByteReadChannel = httpResponse.receive()
                            var fileCumulativeSize = 0
                            while (!channel.isClosedForRead) {
                                val packet = channel.readRemaining(DEFAULT_BUFFER_SIZE.toLong(), 0)
                                while (!packet.isEmpty) {
                                    val bytes = packet.readBytes()
                                    tempFile.appendBytes(bytes)

                                    fileCumulativeSize += bytes.size
                                    progressBar.stepTo(cumulativeSize + fileCumulativeSize)
                                }
                            }
                        }
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
                    if (tempFile.isMissingOrCorrupted(file.size, file.checksum)) {
                        logger.info { "The recently downloaded file ${file.folder}/${file.name} seems to be corrupted, the file has been re added to the download queue" }
                        tempFile.delete()
                        filesToDownload.add(file)
                    } else {
                        localFile.delete()
                        tempFile.renameTo(localFile)

                        // Increase the progress bar
                        cumulativeSize += file.size
                        progressBar.stepTo(cumulativeSize)
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

                // Check if there is enough disk space to allocate all the block files
                logger.info { "Checking the free disk space..." }
                val bytesToDownload = (filesToDownload.sumOf { it.size } * 1.10).toLong()
                val dataDirectoryUsableSpace = Paths.get(dataDirectory).toFile().usableSpace
                if (bytesToDownload > dataDirectoryUsableSpace) {
                    val requiredGbs = "${(bytesToDownload.toDouble().toGb() * 100).roundToInt() / 100.0} Gb"
                    logger.error { "The specified data directory '$dataDirectory' doesn't have enough free disk space, a minimum of $requiredGbs are required to allocate all the blocks, please free up at least $requiredGbs of disk space!" }
                } else {
                    logger.info { "There is enough disk space, downloading the block files..." }
                    downloadFiles(filesToDownload)
                }

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
    val fileLength: Long = length()
    val fileChecksum: String = inputStream().use { DigestUtils.md5Hex(it) }
    val isCorrupted = fileLength != size || !fileChecksum.equals(checksum, ignoreCase = true)
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
    val localUrl: Boolean = false,
    val displayHelp: Boolean = false,
    val autoClose: Boolean = false,
    private val nodecoreDataDirectory: String? = null
) {
    val dataDirectory: String = nodecoreDataDirectory
        ?: getNodeCoreDataDirectory()
}

private fun getNodeCoreDataDirectory(): String {
    // Get the root folder from the package
    val packageParentFolder = Paths.get("").toAbsolutePath().parent?.parent
        ?: return "./"
    logger.info {"Package root folder: $packageParentFolder"}

    // Search the NodeCore folder inside the package
    val nodeCoreFolder = packageParentFolder.toFile().listFiles()?.find {
        it.name.startsWith("nodecore-0") && it.isDirectory && !it.isFile// nodecore-0.x.x...
    } ?: return "./"

    logger.info { "Found the NodeCore package: ${nodeCoreFolder.absolutePath}" }
    return Paths.get(nodeCoreFolder.toPath().toString(), "bin").toString()
}

private fun Double.toGb(): Double = this / 1024/ 1024 / 1024

suspend fun main(args: Array<String>) {
    print(SharedConstants.LICENSE)
    println(SharedConstants.VERIBLOCK_APPLICATION_NAME.replace("$1", ApplicationMeta.FULL_APPLICATION_NAME_VERSION))
    println("\t\t${SharedConstants.VERIBLOCK_WEBSITE}")
    println("\t\t${SharedConstants.VERIBLOCK_EXPLORER}\n")
    println("${SharedConstants.VERIBLOCK_PRODUCT_WIKI_URL.replace("$1", "https://wiki.veriblock.org/index.php/Bootstrap_Downloader")}\n")

    val jvmVersionResult = checkJvmVersion()
    if (!jvmVersionResult.wasSuccessful()) {
        logger.error("JVM version is not correct!")
        logger.error(jvmVersionResult.error)
        return
    }

    val config = Configuration().extract<DownloaderConfig>("downloader")
        ?: DownloaderConfig()

    val networkDirectory = Paths.get(config.dataDirectory, config.network).toFile()
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
        url = config.url,
        network = config.network,
        dataDirectory = config.dataDirectory,
        isLocalUrl = config.localUrl
    ).downloadBlocks()

    if (!config.autoClose) {
        logger.info { "Finished! Press any key to exit..." }
        Scanner(System.`in`).nextLine()
    }
}
