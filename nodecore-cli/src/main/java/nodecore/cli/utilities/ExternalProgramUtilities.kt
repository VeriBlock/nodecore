// VeriBlock NodeCore
// Copyright 2017-2020 Xenios SEZC
// All rights reserved.
// https://www.veriblock.org
// Distributed under the MIT software license, see the accompanying
// file LICENSE or http://www.opensource.org/licenses/mit-license.php.
package nodecore.cli.utilities

import java.io.File

class ExtendedIllegalStateException(
    override val message: String,
    val extraMessage: String
) : IllegalStateException()

object ExternalProgramUtilities {
    @JvmStatic
    fun startupExternalProcess(
        relativeMasterFolderLocation: String,
        masterProgramFolderBeginning: String,
        scriptName: String,
        programName: String
    ): String {
        val isWindows = System.getProperty("os.name").toLowerCase().contains("windows")
        val isMac = System.getProperty("os.name").toLowerCase().contains("mac os x")
        val platformSpecificExtension = if (isWindows) ".bat" else ""
        val scriptFileName = scriptName + platformSpecificExtension
        return try {
            val file = File(relativeMasterFolderLocation)
            val allFiles = file.listFiles()
                ?: throw ExtendedIllegalStateException(
                    "Unable to find $programName binary or containing folder",
                    "The master directory containing " + programName + " (often in the format " + masterProgramFolderBeginning + ".x.x) could\n" +
                        "\tnot be found in the directory \n\t" + file.getCanonicalPath() + ""
                )

            var scriptDirectory: File? = null
            for (subFile in allFiles) {
                if (subFile.isDirectory && subFile.name.startsWith(masterProgramFolderBeginning)) {
                    scriptDirectory = File(subFile, "bin")
                    break
                }
            }
            if (scriptDirectory != null && scriptDirectory.isDirectory) {
                val absoluteBatPath = scriptDirectory.canonicalPath + File.separator + scriptFileName
                val batFile = File(absoluteBatPath)
                if (batFile.isFile && batFile.exists()) {
                    val proc: Process
                    proc = if (isWindows) {
                        ProcessBuilder("cmd", "/C", "start", scriptFileName).directory(scriptDirectory).start()
                    } else if (isMac) {
                        ProcessBuilder("/usr/bin/osascript",
                            "-e", "tell app \"Terminal\"",
                            "-e", "set currentTab to do script " +
                            "(\"cd " + scriptDirectory.canonicalPath + " && chmod a+x " + scriptFileName + " && bash -c ./" + scriptFileName + "\")",
                            "-e", "end tell").start()
                    } else {
                        val fullCommand = "cd " + scriptDirectory.canonicalPath + " && chmod a+x " + scriptFileName + " && x-terminal-emulator -e ./" + scriptFileName
                        ProcessBuilder("bash", "-c", fullCommand).start()
                    }
                    if (proc.isAlive) {
                        absoluteBatPath
                    } else {
                        throw ExtendedIllegalStateException(
                            "Unable to launch " + programName + "!",
                            programName + " The Runtime Environment reported that starting " + absoluteBatPath + " was unsuccesful!"
                        )
                    }
                } else {
                    throw ExtendedIllegalStateException(
                        "Unable to find " + programName + " binary or containing folder",
                        "The batch/script file\n\t" + scriptDirectory.canonicalPath + scriptName + " does not exist!"
                    )
                }
            } else {
                if (scriptDirectory == null) {
                    throw ExtendedIllegalStateException(
                        "Unable to find " + programName + " binary or containing folder",
                        "The master directory containing " + programName + " (often in the format " + masterProgramFolderBeginning + "-x.x.x) could\n" +
                            "\tnot be found in the directory \n\t" + file.getCanonicalPath() + "."
                    )
                } else {
                    throw ExtendedIllegalStateException(
                        "Unable to find " + programName + " binary or containing folder",
                        "The directory " + scriptDirectory.getCanonicalPath() + " does not contain a startup script for " + programName + "."
                    )
                }
            }
        } catch (e: Exception) {
            throw ExtendedIllegalStateException(
                "Unable to find " + programName + " binary or containing folder",
                "The following error was encountered: " + e.message
            )
        }
    }
}
