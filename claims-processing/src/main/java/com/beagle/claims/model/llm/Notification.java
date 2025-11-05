package com.beagle.claims.model.llm;

import lombok.Data;

@Data
public class Notification {
    private Boolean present;
    private String date;
    private String evidence;
}