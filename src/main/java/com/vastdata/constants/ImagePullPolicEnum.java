package com.vastdata.constants;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum ImagePullPolicEnum {
    A("Always"),
    IP("IfNotPresent "),
    N("Never "),
    ;

    private final String polic;
}
