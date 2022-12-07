package com.example.taxreport;

import lombok.*;
import lombok.extern.slf4j.Slf4j;

import java.net.URLDecoder;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Slf4j
public class TaxLineRow {

    private String id;
    private String[] data;

    private boolean mergedBefore = false;

    private int jsonIndex = -1;

    public void print () {
        String finalString = "";

        for (String i : data) {
            finalString += i + "\t || \t";
        }

        log.info("line size {} - {}", data.length, finalString);
    }
}
