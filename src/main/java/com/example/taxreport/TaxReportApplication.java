package com.example.taxreport;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

import java.io.IOException;

@SpringBootApplication
@Slf4j
public class TaxReportApplication {

    public static void main(String[] args) throws IOException {
        SpringApplication.run(TaxReportApplication.class, args);
        CSVRowMergerProcessor processor = new CSVRowMergerProcessor();

      if(args == null || args.length < 2){
            log.error("input and output folder not properly defined");
            log.warn("usage: java -jar jarname.jar 'path_to_input_folder' 'path_to_output_folder'");
            log.warn("escape folder path if running in a windows machine");
            log.warn("input and output folders MUST exist in the file system");
            return;
        }

        String inputFolder = args[0];
        log.info("Defined input folder: {}", inputFolder);
        String outputFolder = args[1];
        log.info("Defined output folder: {}", outputFolder);

        if(inputFolder == null || outputFolder == null){
            return;
        }

        if(!inputFolder.isEmpty()) {
            processor.setCsvFolder(inputFolder);
        }
        if(!outputFolder.isEmpty()) {
            processor.setCSVNewFOLDER(outputFolder);
        }

        processor.run();
    }

}
