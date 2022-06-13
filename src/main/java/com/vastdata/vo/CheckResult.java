package com.vastdata.vo;

import lombok.Data;

@Data
public class CheckResult {
    boolean match;
    boolean replace;
    boolean rollingUpdate;
    String reasons;
}
