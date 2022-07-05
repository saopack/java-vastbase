package com.vastdata.constants;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum ResourceKindEnum {
    PV("PersistentVolume"),
    PVC("PersistentVolumeClaim"),
    
    POD("Pod"),

    CONFIG_MAP("ConfigMap"),

    STATEFUL_SET("StatefulSet"),

    SERVICE("Service"),
    ;

    private final String kind;
}
