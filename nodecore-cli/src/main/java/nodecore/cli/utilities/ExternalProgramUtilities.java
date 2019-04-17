// VeriBlock NodeCore
// Copyright 2017-2019 Xenios SEZC
// All rights reserved.
// https://www.veriblock.org
// Distributed under the MIT software license, see the accompanying
// file LICENSE or http://www.opensource.org/licenses/mit-license.php.

package nodecore.cli.utilities;

import nodecore.cli.contracts.Result;

import java.io.File;

public class ExternalProgramUtilities {
    public static String startupExternalProcess(Result result, String relativeMasterFolderLocation, String masterProgramFolderBeginning, String scriptName, String programName) {
        boolean isWindows = (System.getProperty("os.name").toLowerCase().contains("windows"));
        boolean isMac = (System.getProperty("os.name").toLowerCase().contains("mac os x"));
        String platformSpecificExtension = isWindows ? ".bat" : "";

        scriptName += platformSpecificExtension;
        try {
            File file = new File(relativeMasterFolderLocation);
            File[] allFiles = file.listFiles();

            if (allFiles == null) {
                result.addMessage(
                        "V004",
                        "Unable to find " + programName + " binary or containing folder",
                        "The master directory containing " + programName + " (often in the format " + masterProgramFolderBeginning + ".x.x) could\n" +
                                "\tnot be found in the directory \n\t" + file.getCanonicalPath() + "",
                        true);
                result.fail();
                return null;
            }


            File scriptDirectory = null;
            for (File subFile : allFiles) {
                if (subFile.isDirectory() && subFile.getName().startsWith(masterProgramFolderBeginning)) {
                    scriptDirectory = new File(subFile, "bin");
                    break;
                }
            }


            if (scriptDirectory != null && scriptDirectory.isDirectory()) {
                String absoluteBatPath = scriptDirectory.getCanonicalPath() + File.separator + scriptName;
                File batFile = new File(absoluteBatPath);
                if (batFile.isFile() && batFile.exists()) {
                    Process proc;
                    if (isWindows) {
                        proc = new ProcessBuilder("cmd", "/C", "start", scriptName).directory(scriptDirectory).start();
                    } else if (isMac) {
                        proc = new ProcessBuilder("/usr/bin/osascript",
                                "-e", "tell app \"Terminal\"",
                                "-e", "set currentTab to do script " +
                                "(\"cd " + scriptDirectory.getCanonicalPath() + " && chmod a+x " + scriptName + " && bash -c ./" + scriptName + "\")",
                                "-e", "end tell").start();

                    } else {
                        String fullCommand = "cd " + scriptDirectory.getCanonicalPath() + " && chmod a+x " + scriptName + " && x-terminal-emulator -e ./" + scriptName;
                        proc = new ProcessBuilder("bash", "-c", fullCommand).start();
                    }

                    if (proc.isAlive()) {
                        return absoluteBatPath;
                    } else {
                        result.addMessage(
                                "V004",
                                "Unable to launch " + programName + "!",
                                programName + " The Runtime Environment reported that starting " + absoluteBatPath + " was unsuccesful!",
                                true);
                        return null;
                    }
                } else {
                    result.addMessage(
                            "V004",
                            "Unable to find " + programName + " binary or containing folder",
                            "The batch/script file\n\t" + scriptDirectory.getCanonicalPath() + scriptName + " does not exist!",
                            true);
                    return null;
                }
            } else {
                if (scriptDirectory == null) {
                    result.addMessage(
                            "V004",
                            "Unable to find " + programName + " binary or containing folder",
                            "The master directory containing " + programName + " (often in the format " + masterProgramFolderBeginning + "-x.x.x) could\n" +
                                    "\tnot be found in the directory \n\t" + file.getCanonicalPath() + ".",
                            true);
                    return null;
                } else {
                    result.addMessage(
                            "V004",
                            "Unable to find " + programName + " binary or containing folder",
                            "The directory " + scriptDirectory.getCanonicalPath() + " does not contain a startup script for " + programName + ".",
                            true);
                    return null;
                }
            }
        } catch (Exception e) {
            result.addMessage(
                    "V004",
                    "Unable to find " + programName + " binary or containing folder",
                    "The following error was encountered: " + e.getMessage(),
                    true);
            return null;
        }
    }
}