package com.vastdata.constants;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum ResourceStatus {
    PROCESSING("processing"),
    ;
    private final String status;
}
