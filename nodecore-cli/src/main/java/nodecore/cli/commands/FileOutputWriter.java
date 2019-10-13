// VeriBlock NodeCore
// Copyright 2017-2019 Xenios SEZC
// All rights reserved.
// https://www.veriblock.org
// Distributed under the MIT software license, see the accompanying
// file LICENSE or http://www.opensource.org/licenses/mit-license.php.

package nodecore.cli.commands;

import com.google.gson.GsonBuilder;
import com.opencsv.CSVWriter;
import com.opencsv.bean.StatefulBeanToCsv;
import com.opencsv.bean.StatefulBeanToCsvBuilder;
import com.opencsv.exceptions.CsvDataTypeMismatchException;
import com.opencsv.exceptions.CsvRequiredFieldEmptyException;
import nodecore.cli.commands.serialization.FormattableObject;
import nodecore.cli.contracts.CommandContext;
import nodecore.cli.contracts.OutputWriter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;

public class FileOutputWriter implements OutputWriter {

    private static final Logger _logger = LoggerFactory.getLogger(FileOutputWriter.class);

    @Override
    public void outputObject(CommandContext context, FormattableObject o) {
        String filename =  context.getParameter(DefaultCommandFactory.FILENAME_SELECTOR);
        if(filename == null) {
            return;
        }

        String format = resolveFormat(filename, context);
        saveOutputToFile(filename, o.payload, format);
    }

    private String resolveFormat(String filename, CommandContext context) {
        // TODO: Temporarily always returning JSON

        if(filename.endsWith("csv")) {
            return "csv";
        } else if(filename.endsWith("json")) {
            return "json";
        } else if (context.getParameter(DefaultCommandFactory.FORMAT_SELECTOR) != null) {
            return context.getParameter(DefaultCommandFactory.FORMAT_SELECTOR);
        }

        return "json";
    }

    private void saveOutputToFile(String filename, Object payload, String format) {

        switch (format) {
            case "json":
                saveToJson(filename, payload);
                break;

            case "csv":
                saveToCSV(filename, payload);
                break;

            default:
                saveToJson(filename, payload);
                break;
        }
    }

    @SuppressWarnings("unchecked")
    private void saveToCSV(String filename, Object payload) {

        try (Writer writer  = new FileWriter(filename)) {
            StatefulBeanToCsv sbc = new StatefulBeanToCsvBuilder(writer)
                    .withSeparator(CSVWriter.DEFAULT_SEPARATOR)
                    .build();

            sbc.write(payload);
        } catch (FileNotFoundException e) {
            _logger.error("Can't open output file %s", filename);
        } catch (IOException e) {
            _logger.error("IO Error writing into a file %s", filename);
        } catch (CsvRequiredFieldEmptyException  | CsvDataTypeMismatchException e) {
            _logger.error("CSV format error for a file %s", filename);
        }
    }

    private void saveToJson(String filename, Object payload) {
        try {
            PrintWriter out = new PrintWriter(filename);
            out.print(new GsonBuilder().setPrettyPrinting().create().toJson(payload));
            out.flush();
            out.close();
        } catch (FileNotFoundException e) {
            _logger.error("Can't open output file %s", filename);
        }
    }
}
