package com.example.taxreport;

import lombok.*;
import lombok.extern.slf4j.Slf4j;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Slf4j
public class TaxLineHeader {

    private int pivotIndex = -1;
    private String[] data;

    public void print () {
        String finalString = "";

        for (String i : data) {
            finalString += i + "\t || \t";
        }

        log.info("line size {} - {}", data.length, finalString);
    }
}
