// VeriBlock NodeCore
// Copyright 2017-2020 Xenios SEZC
// All rights reserved.
// https://www.veriblock.org
// Distributed under the MIT software license, see the accompanying
// file LICENSE or http://www.opensource.org/licenses/mit-license.php.
package org.veriblock.shell

import com.google.gson.GsonBuilder
import com.opencsv.CSVWriter
import com.opencsv.bean.StatefulBeanToCsv
import com.opencsv.bean.StatefulBeanToCsvBuilder
import com.opencsv.exceptions.CsvDataTypeMismatchException
import com.opencsv.exceptions.CsvRequiredFieldEmptyException
import org.veriblock.core.utilities.createLogger
import java.io.FileNotFoundException
import java.io.FileWriter
import java.io.IOException
import java.io.PrintWriter

private val logger = createLogger {}

class FileOutputWriter {
    fun outputObject(o: Any, filename: String, format: String? = null) {
        saveOutputToFile(filename, o, resolveFormat(filename, format))
    }

    private fun resolveFormat(filename: String, format: String?): String {
        if (filename.endsWith("csv")) {
            return "csv"
        } else if (filename.endsWith("json")) {
            return "json"
        } else if (format != null) {
            return format
        }
        // TODO: Temporarily always returning JSON
        return "json"
    }

    private fun saveOutputToFile(filename: String, payload: Any, format: String) {
        when (format) {
            "json" -> saveToJson(filename, payload)
            "csv" -> saveToCSV(filename, payload)
            else -> saveToJson(filename, payload)
        }
    }

    private fun saveToCSV(filename: String, payload: Any) {
        try {
            FileWriter(filename).use { writer ->
                val sbc: StatefulBeanToCsv<Any> = StatefulBeanToCsvBuilder<Any>(writer)
                    .withSeparator(CSVWriter.DEFAULT_SEPARATOR)
                    .build()
                sbc.write(payload)
            }
        } catch (e: FileNotFoundException) {
            logger.error("Can't open output file {}", filename)
        } catch (e: IOException) {
            logger.error("IO Error writing into a file {}", filename)
        } catch (e: CsvRequiredFieldEmptyException) {
            logger.error("CSV format error for a file {}", filename)
        } catch (e: CsvDataTypeMismatchException) {
            logger.error("CSV format error for a file {}", filename)
        }
    }

    private fun saveToJson(filename: String, payload: Any) {
        try {
            PrintWriter(filename).use { out ->
                out.print(GsonBuilder().setPrettyPrinting().create().toJson(payload))
                out.flush()
            }
        } catch (e: FileNotFoundException) {
            logger.error("Can't open output file {}", filename)
        }
    }
}
