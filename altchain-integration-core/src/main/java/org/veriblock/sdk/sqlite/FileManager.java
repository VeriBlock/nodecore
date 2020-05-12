// VeriBlock Blockchain Project
// Copyright 2017-2018 VeriBlock, Inc
// Copyright 2018-2019 Xenios SEZC
// All rights reserved.
// https://www.veriblock.org
// Distributed under the MIT software license, see the accompanying
// file LICENSE or http://www.opensource.org/licenses/mit-license.php.

package org.veriblock.sdk.sqlite;

import java.io.File;
import java.nio.file.Paths;

public class FileManager {

    private FileManager() { }

    private static final String DATA_FOLDER = "data";
    private static final String TEMP_FOLDER = "tmp";

    public static String getRootDirectory()
    {
        return "./";
    }

    public static String getDataDirectory()
    {
        String s = getRootDirectory();
        String sPath = Paths.get(s, DATA_FOLDER).toString();
        ensureDirectoryExists(sPath, "data");
        return sPath;
    }
    
    public static String getTempDirectory()
    {
        String s = getRootDirectory();
        String sPath = Paths.get(s, TEMP_FOLDER).toString();
        ensureDirectoryExists(sPath, "temp");
        return sPath;
    }

    private static void ensureDirectoryExists(String sPath, String purpose)
    {
        if (!doesFolderExist(sPath))
        {
            createFolder(sPath);
        }
    }
    
    public static boolean doesFolderExist(String directoryName)
    {
        File file = new File(directoryName);
        if (file.exists() && file.isDirectory()) {
            return true;
        }
        else
        {
            return false;
        }
    }

    //No change if folder already exists. Else creates folder.
    public static void createFolder(String directoryName)
    {
        File directory = new File(directoryName);
        if (!directory.exists()) {
            directory.mkdir();
        }
    }
}