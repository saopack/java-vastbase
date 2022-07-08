package com.vastdata.service;

import com.vastdata.cluster.VastbaseCluster;
import lombok.extern.slf4j.Slf4j;

import javax.enterprise.context.ApplicationScoped;

@Slf4j
@ApplicationScoped
public class EventService {
    public void clusterMaintainStart(VastbaseCluster resource) {
    }

    public void clusterMaintainEnd(VastbaseCluster resource) {
    }

    public void InstanceConfigFail(VastbaseCluster resource, String name, String s) {

    }

    public void InstanceSetPrimary(VastbaseCluster resource, String name, String s) {

    }
}
