package com.vastdata.cluster;

import com.vastdata.vo.CheckResult;
import io.fabric8.kubernetes.api.model.Pod;
import io.fabric8.kubernetes.api.model.Service;
import io.fabric8.kubernetes.api.model.apps.StatefulSet;
import io.fabric8.kubernetes.client.KubernetesClient;
import lombok.extern.slf4j.Slf4j;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import java.util.List;

@Slf4j
@ApplicationScoped
public class SyncHandler {
    @Inject
    private ResourcesCheck resourcesCheck;
    @Inject
    private ResourcesBuild resourcesBuild;

    public void syncCluster(VastbaseCluster resource, KubernetesClient client) {
        log.info("[{}:{}]开始处理集群", resource.getSpec().getNamespace(), resource.getCRDName());
        var spec = resource.getSpec();
        // 新建readService
        final var readService = resourcesBuild.buildReadService(resource);
        // Step 4 新建readwriteService
        final var writeService = resourcesBuild.buildWriteService(resource);
        log.info("spec: " + spec.toString());
        var readServiceResource = client.resources(Service.class).withName(spec.getVastbaseReadServiceName());
        var writeServiceResource = client.resources(Service.class).withName(spec.getVastbaseWriteServiceName());

        // 判断这些资源是不是存在，可以用断言，代码好看一些
        var readServiceExisting = readServiceResource.get();
        var writeServiceExisting = writeServiceResource.get();

        // 资源不存在就创建
        CheckResult checkResult = new CheckResult();
        // 检查read service
        checkResult = resourcesCheck.checkReadWriteService(readServiceExisting, readService);
        if (!checkResult.isMatch()) {
            log.info("readService name of {} was created!", readService.getMetadata().getName());
            readServiceResource.createOrReplace(readService);
        }
        // 检查readwrite service
        checkResult = resourcesCheck.checkReadWriteService(writeServiceExisting, writeService);
        if (!checkResult.isMatch()) {
            log.info("writeService name of {} was created!", writeService.getMetadata().getName());
            writeServiceResource.createOrReplace(writeService);
        }
        // 维护vastbase G100集群
        List<Pod> readyPodList = resourcesCheck.ensureDBCluster(client, resource);
        if (readyPodList.size() < 1) {
            return;
        }
        // 集群选主, 以ready的节点作为集群
        checkResult = resourcesCheck.ensurePrimary(client, resource, readyPodList);
        if (!checkResult.isMatch()) {
            log.info("选主完成,主节点为:{}", checkResult.getReasons());
        }
    }
}
