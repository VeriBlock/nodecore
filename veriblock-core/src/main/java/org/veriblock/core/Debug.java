package org.veriblock.core;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.veriblock.core.wallet.AddressManager;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.io.StringWriter;

public class Debug {
    private static final Logger logger = LoggerFactory.getLogger(Debug.class);
    public static void main(String[] args) throws IOException {
        String installedPackages = getInstalledPackagesUbuntu();
        logger.info("Installed packages (Ubuntu):");
        logger.info(installedPackages);

        AddressManager manager = new AddressManager();
        manager.load(new File("wallet.dat"));
        for (int i = 0; i < 100_000; i++) {
            int entropyBefore = Integer.parseInt(getEntropy().replaceAll("\"", "").trim());
            long start = System.currentTimeMillis();
            String address = manager.getNewAddress().getHash();
            int entropyAfter = Integer.parseInt(getEntropy().replaceAll("\"", "").trim());
            logger.info("Generating address #" + i + " took " + (System.currentTimeMillis() - start) + "ms. Entropy before: " + entropyBefore + ", entropy after: " + entropyAfter);
        }
    }

    public static String getEntropy() {
        String line = null;
        String strstatus = "";
        try {

            String[] cmd = {"/bin/sh", "-c", "cat /proc/sys/kernel/random/entropy_avail"};
            Process p = Runtime.getRuntime().exec(cmd);
            BufferedReader in = new BufferedReader(new InputStreamReader(p.getInputStream()));
            while ((line = in.readLine()) != null) {
                strstatus += line + "\n";
            }
            in.close();
        } catch (Exception e) {

            StringWriter sw = new StringWriter();
            PrintWriter pw = new PrintWriter(sw);
            e.printStackTrace(pw);
            pw.flush();
            String stackTrace = sw.toString();
            int lenoferrorstr = stackTrace.length();
            if (lenoferrorstr > 500) {
                strstatus = "Error:" + stackTrace.substring(0, 500);
            } else {
                strstatus = "Error:" + stackTrace.substring(0, lenoferrorstr - 1);
            }
        }
        return strstatus;
    }

    public static String getInstalledPackagesUbuntu() {
        String line = null;
        String strstatus = "";
        try {

            String[] cmd = {"/bin/sh", "-c", "dpkg -l"};
            Process p = Runtime.getRuntime().exec(cmd);
            BufferedReader in = new BufferedReader(new InputStreamReader(p.getInputStream()));
            while ((line = in.readLine()) != null) {
                strstatus += line + "\n";
            }
            in.close();
        } catch (Exception e) {

            StringWriter sw = new StringWriter();
            PrintWriter pw = new PrintWriter(sw);
            e.printStackTrace(pw);
            pw.flush();
            String stackTrace = sw.toString();
            int lenoferrorstr = stackTrace.length();
            if (lenoferrorstr > 500) {
                strstatus = "Error:" + stackTrace.substring(0, 500);
            } else {
                strstatus = "Error:" + stackTrace.substring(0, lenoferrorstr - 1);
            }
        }
        return strstatus;
    }
}
