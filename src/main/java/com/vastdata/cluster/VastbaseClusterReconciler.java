package com.vastdata.cluster;


import com.vastdata.vo.CheckResult;
import io.fabric8.kubernetes.api.model.*;
import io.fabric8.kubernetes.api.model.apps.StatefulSet;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.javaoperatorsdk.operator.api.reconciler.Context;
import io.javaoperatorsdk.operator.api.reconciler.Reconciler;
import io.javaoperatorsdk.operator.api.reconciler.UpdateControl;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;

public class VastbaseClusterReconciler implements Reconciler<VastbaseCluster> {
    private final KubernetesClient client;
    private static final Logger log = LoggerFactory.getLogger(VastbaseClusterReconciler.class);
    @Inject
    private ResourcesCheck resourcesCheck;
    @Inject
    private ResourcesBuild resourcesBuild;
    public VastbaseClusterReconciler(KubernetesClient client) {
        this.client = client;
    }

    // TODO Fill in the rest of the reconciler

    @Override
    public UpdateControl<VastbaseCluster> reconcile(VastbaseCluster resource, Context context) {
        var spec = resource.getSpec();
        // Step 1 新建PV
        final var persistentVolume = resourcesBuild.buildPersistentVolume(resource);
        // Step 2 新建PVC
        final var persistentVolumeClaim = resourcesBuild.buildPersistentVolumeClaim(resource);
        // Step 3 新建Pod-StatefulSet
        final var statefulSet = resourcesBuild.buildStatefulSet(resource);
        // Step 4 新建Headless Service
        final var headlessService = resourcesBuild.buildHeadlessService(resource);
        // Step 5 新建readService
        final var readService = resourcesBuild.buildReadService(resource);
        // Step 5 新建readwriteService
        final var writeService = resourcesBuild.buildWriteService(resource);
        log.info("spec: " + spec.toString());
        var secretResource = client.resources(Secret.class).withName(spec.getSecretName());
        var pvResource = client.resources(PersistentVolume.class).withName(spec.getPvName());
        var pvcResource = client.resources(PersistentVolumeClaim.class).withName(spec.getPvcName());
        var statefulSetResource = client.resources(StatefulSet.class).withName(spec.getStatefulSetName());
        var headlessServiceResource = client.resources(Service.class).withName(spec.getHeadlessServiceName());
        var readServiceResource = client.resources(Service.class).withName(spec.getVastbaseReadServiceName());
        var writeServiceResource = client.resources(Service.class).withName(spec.getVastbaseWriteServiceName());

        // 判断这些资源是不是存在，可以用断言，代码好看一些
        var secretExisting = secretResource.get();
        var pvcExisting = pvcResource.get();
        var pvExisting = pvResource.get();
        var statefulSetExisting = statefulSetResource.get();
        var headlessServiceExisting = headlessServiceResource.get();
        var readServiceExisting = readServiceResource.get();
        var writeServiceExisting = writeServiceResource.get();

        // 资源不存在就创建
        CheckResult checkResult = new CheckResult();
        // 检查pv
        checkResult = resourcesCheck.checkPV(pvExisting, persistentVolume);
        if (!checkResult.isMatch()) {
            log.info("pv name of {} was created!", persistentVolume.getMetadata().getName());
            pvResource.createOrReplace(persistentVolume);
        }
        // 检查pvc
        checkResult = resourcesCheck.checkPVC(pvcExisting, persistentVolumeClaim);
        if (!checkResult.isMatch()) {
            log.info("pvc name of {} was created!", persistentVolumeClaim.getMetadata().getName());
            pvcResource.createOrReplace(persistentVolumeClaim);
        }
        // 检查Secret
        checkResult = resourcesCheck.checkSecret(secretExisting, persistentVolumeClaim);
        if (!checkResult.isMatch()) {
            log.info("pv name of {} was created!", persistentVolumeClaim.getMetadata().getName());
            pvcResource.createOrReplace(persistentVolumeClaim);
        }
        // 检查headless service
        checkResult = resourcesCheck.checkHeadlessService(headlessServiceExisting, headlessService);
        if (!checkResult.isMatch()) {
            log.info("headlessService name of {} was created!", headlessService.getMetadata().getName());
            headlessServiceResource.createOrReplace(headlessService);
        }
        // 检查read service
        checkResult = resourcesCheck.checkReadService(readServiceExisting, readService);
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
        // 检查statefulset
        checkResult = resourcesCheck.checkStatefulSet(statefulSetExisting, statefulSet);
        if (!checkResult.isMatch()) {
            log.info("statefulSet name of {} was created!", statefulSet.getMetadata().getName());
            statefulSetResource.createOrReplace(statefulSet);
        }
        return UpdateControl.noUpdate();
    }

    
}

