package com.vastdata.constants;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum ApiVersionEnum {
    V1("v1"),
    APPS_V1("apps/v1"),
    ;

    private final String apiVersion;
}
