package com.vastdata.vo;

import lombok.Data;

@Data
public class CompareStatefulsetResult {
    boolean match;
    boolean replace;
    boolean rollingUpdate;
    String reasons;
}
