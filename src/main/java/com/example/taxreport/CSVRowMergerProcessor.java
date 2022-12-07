package com.example.taxreport;

import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.io.*;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.*;

@NoArgsConstructor
@Slf4j
public class CSVRowMergerProcessor {
    private static final String CSV_SUFFIX = ".csv";
    private String csvFolder;
    private String csvNewFolder;
    private static final String DELIMITER = ",";

    private static final String JSON_INIT = ",\"{\"";
    private static final String JSON_END = "}\",";
    private static final String SYSTEM_DELIMITER = File.separator;

    private static final String PIVOT_COLUMN = "id";

    public void run() {
        File directoryPath = new File(csvFolder);
        String[] contents = directoryPath.list();
        for (int i = 0; i < Objects.requireNonNull(contents).length; i++) {
            if (contents[i].endsWith(CSV_SUFFIX)) {
                processFile(contents[i]);
            }
        }
    }

    public void processFile(String fileName) {
        LinkedHashMap<String, TaxLineRow> taxLineRowHashMap = new LinkedHashMap<>();

        try (BufferedReader br = new BufferedReader(new FileReader(csvFolder + fileName))) {
            String row = br.readLine();
            TaxLineHeader taxLineHeader = convertToTaxLineHeader(row);

            while ((row = br.readLine()) != null) {
                TaxLineRow taxLineRow = processRowAndConvertToTaxLine(row, taxLineHeader.getPivotIndex());
                TaxLineRow storedTaxLineRow = taxLineRowHashMap.get(taxLineRow.getId());
                if (storedTaxLineRow != null) {
                    boolean mergedBefore = storedTaxLineRow.isMergedBefore();
                    if(mergedBefore){
                        log.warn("[{}] \t Report has invalid data, order id {} already exists", fileName, storedTaxLineRow.getId());
                    }else {
                        TaxLineRow mergedTaxLineRow = mergeTaxLineRows(taxLineRow, storedTaxLineRow);
                        taxLineRowHashMap.put(mergedTaxLineRow.getId(), mergedTaxLineRow);
                    }
                } else {
                    taxLineRowHashMap.put(taxLineRow.getId(), taxLineRow);
                }
            }

            createAndWriteToFile(fileName, taxLineRowHashMap, taxLineHeader);

        } catch (IOException e) {
            log.error(e.getMessage());
        }
    }

    private TaxLineRow mergeTaxLineRows(TaxLineRow taxLineRow, TaxLineRow storedTaxLineRow) {
        String[] newData;

        if (taxLineRow.getData().length > storedTaxLineRow.getData().length) {
            newData = mergeDataRows(taxLineRow.getData(), storedTaxLineRow.getData());
        } else {
            newData = mergeDataRows(storedTaxLineRow.getData(), taxLineRow.getData());
        }

        return new TaxLineRow.TaxLineRowBuilder()
                .data(newData)
                .id(taxLineRow.getId())
                .mergedBefore(true)
                .jsonIndex(checkForEscapedJson(newData))
                .build();
    }

    private String[] mergeDataRows(String[] biggerRow, String[] smallerRow) {
        int maxIndex = biggerRow.length;
        String[] newData = new String[maxIndex];

        for (int i = 0; i < maxIndex; i++) {

            String biggerColumn = biggerRow[i];
            String smallerColumn = null;

            if (smallerRow.length > i) {
                smallerColumn = smallerRow[i];
            }

            newData[i] = (biggerColumn != null && !biggerColumn.isEmpty()) ? biggerColumn : smallerColumn;
        }

        return newData;
    }

    private TaxLineRow processRowAndConvertToTaxLine(String row, int pivotIndex){

        String escapedRow = escapeJsonColumns(row);
        String[] data = splitLine(escapedRow);

        return new TaxLineRow.TaxLineRowBuilder()
                .data(data)
                .id(data[pivotIndex])
                .jsonIndex(checkForEscapedJson(data))
                .build();
    }

    private int checkForEscapedJson(String[] data){
        for(int i=0; i<data.length; i++){
            String datum = data[i];
            boolean b = datum.startsWith("%");
            if(b){
                return i;
            }
        }
        return -1;
    }

    //FIXME: For now, it only supports 1 json field in the report, if multiple fields are required, we need to loop for all of them
    private String escapeJsonColumns(String row) {
        boolean contains = row.contains(JSON_INIT);
        if (contains) {
            int initIndex = row.indexOf(JSON_INIT) + 1;
            int endIndex = row.indexOf(JSON_END) + 2;
            String substring = row.substring(initIndex, endIndex);

            String encodedString = URLEncoder.encode(substring, StandardCharsets.UTF_8);
            return row.substring(0, initIndex) + encodedString + row.substring(endIndex);
        } else {
            return row;
        }
    }

    private TaxLineHeader convertToTaxLineHeader(String row) {
        return new TaxLineHeader.TaxLineHeaderBuilder()
                .data(splitLine(row))
                .pivotIndex(getHeaderIndex(row))
                .build();
    }

    private int getHeaderIndex(String line) {
        String[] headers = splitLine(line);
        int index = -1;
        for (int i = 0; i < headers.length; i++) {
            if (CSVRowMergerProcessor.PIVOT_COLUMN.equalsIgnoreCase(headers[i])) {
                index = i;
                break;
            }
        }

        return index;
    }

    private String[] splitLine(String line) {
        return line.split(DELIMITER);
    }


    public void setCsvFolder(String str) {
        if (!str.endsWith(SYSTEM_DELIMITER)) {
            str = str + SYSTEM_DELIMITER;
        }
        csvFolder = str;
    }

    public void setCSVNewFOLDER(String str) {
        if (!str.endsWith(SYSTEM_DELIMITER)) {
            str = str + SYSTEM_DELIMITER;
        }
        csvNewFolder = str;
    }

    private void createAndWriteToFile(String filename, LinkedHashMap<String, TaxLineRow> taxLineRowHashMap, TaxLineHeader taxLineHeader) throws IOException {
        File createdFile = new File(csvNewFolder + filename);
        if (createdFile.createNewFile()) {
            log.info("[{}] \t File created at: {} ", filename, createdFile.getAbsolutePath());
        } else {
            log.error("[{}] \t File already exists.", filename);
            return;
        }

        try (FileWriter myWriter = new FileWriter(csvNewFolder + filename)) {
            myWriter.write(buildFinalString(taxLineHeader.getData(), -1) + "\n");

            for (TaxLineRow line : taxLineRowHashMap.values()) {
                myWriter.write(buildFinalString(line.getData(), line.getJsonIndex()) + "\n");
            }
        } catch (IOException e) {
            log.error("[{}] \t Error writing to file", filename);
        }

    }

    public String buildFinalString(String[] data, int jsonIndex){
        StringBuilder finalString = new StringBuilder();
        for(int i=0; i<data.length; i++){
            if(i == jsonIndex){
                finalString.append(URLDecoder.decode(data[i], StandardCharsets.UTF_8));
            }else{
                finalString.append(data[i]);
            }

            if(i != data.length -1){
                finalString.append(DELIMITER);
            }

        }
        return finalString.toString();
    }

}
