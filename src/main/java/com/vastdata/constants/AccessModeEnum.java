package com.vastdata.constants;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum AccessModeEnum {
    RWO("ReadWriteOnce"),

    ;
    private String accessMode;
}
