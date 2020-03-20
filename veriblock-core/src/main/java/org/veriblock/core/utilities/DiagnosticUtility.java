// VeriBlock NodeCore
// Copyright 2017-2020 Xenios SEZC
// All rights reserved.
// https://www.veriblock.org
// Distributed under the MIT software license, see the accompanying
// file LICENSE or http://www.opensource.org/licenses/mit-license.php.

package org.veriblock.core.utilities;

import org.veriblock.core.types.SimpleResult;

import java.text.SimpleDateFormat;
import java.util.Date;

public class DiagnosticUtility
{
    public static DiagnosticInfo getDiagnosticInfo()
    {
        DiagnosticInfo d = new DiagnosticInfo();

        try {
            //Runtime.getRuntime().availableProcessors();

            d.user_language = System.getProperty("user.language");
            d.java_runtime_name = System.getProperty("java.runtime.name");
            d.sun_boot_library_path = System.getProperty("sun.boot.library.path");
            d.java_runtime_version = System.getProperty("java.runtime.version");
            d.working_directory =  System.getProperty("user.dir");

            d.java_specification_version = System.getProperty("java.specification.version");
            d.os_name = System.getProperty("os.name");
            d.os_arch = System.getProperty("os.arch");
            d.os_version = System.getProperty("os.version");
            d.memory_total_gb = String.format("%.2f", (double) Runtime.getRuntime().totalMemory() / (1024 * 1024 * 1024)) + " GB";
            d.memory_max_gb = String.format("%.2f", (double) Runtime.getRuntime().maxMemory() / (1024 * 1024 * 1024)) + " GB";

            d.processor_count = Integer.toString(Runtime.getRuntime().availableProcessors());
            d.processor_type =  System.getenv().get("PROCESSOR_IDENTIFIER");

            //capture global UTC. Logs should also be UTC, but this is one-single place, and allows easy verification. Checks-and-balances.
            SimpleDateFormat dateFormatLocal = new SimpleDateFormat("YYYY-MM-dd HH:mm:ss.SSSXX");
            Date d1 = new Date();
            d.datetime_now_utc = dateFormatLocal.format(d1);
        }
        catch  (Exception ex) {
            //don't crash on diagnostics info
        }
        return d;
    }

    public static SimpleResult checkIfCorrectVersion() {

        String JVMEnvironmentError = "";
        boolean wrongVersion = false;
        if (!System.getProperty("java.runtime.name").toLowerCase().equals("java(tm) se runtime environment")) {
            JVMEnvironmentError += "ERROR: The NodeCore Suite does not support non-Oracle versions of NodeCore like OpenJDK!\n";
            wrongVersion = true;
        }

        if (!System.getProperty("java.specification.version").toLowerCase().equals("1.8")) {
            JVMEnvironmentError += "ERROR: The NodeCore Suite only supports Java 8 at this time; Java 9, 10, and 11 are not supported!\n";
            wrongVersion = true;
        }

        if (wrongVersion) {
            JVMEnvironmentError += "In order to continue, please download Oracle Java 8u181!\n" +
                    "For Ubuntu/Debian, follow an installation guide like: \n" +
                    "\thttp://tipsonubuntu.com/2016/07/31/install-oracle-java-8-9-ubuntu-16-04-linux-mint-18/\n" +
                    "On Windows, download the 64-bit installer from: \n" +
                    "\thttp://www.oracle.com/technetwork/java/javase/downloads/jdk8-downloads-2133151.html\n" +
                    "Please see https://wiki.veriblock.org/index.php?title=NodeCore_Operations for more details.";
        }

        return new SimpleResult(!wrongVersion, JVMEnvironmentError);
    }

}

