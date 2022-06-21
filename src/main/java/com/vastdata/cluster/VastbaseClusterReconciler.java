package com.vastdata.cluster;


import com.vastdata.vo.CheckResult;
import io.fabric8.kubernetes.api.model.*;
import io.fabric8.kubernetes.api.model.apps.StatefulSet;
import io.fabric8.kubernetes.client.CustomResource;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.javaoperatorsdk.operator.api.reconciler.Context;
import io.javaoperatorsdk.operator.api.reconciler.Reconciler;
import io.javaoperatorsdk.operator.api.reconciler.UpdateControl;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import java.sql.Time;

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
        //查询cr
        var cluster = client.resources(CustomResource.class).withName(resource.getMetadata().getName());
        
        //TODO  cr处理逻辑如果CR不存在，则结束
       /* if (cluster.get()==null){
            return UpdateControl.noUpdate();
        }

        //如果CR已被删除，进行相应资源的清理操作
        if (cluster.get().getMetadata().getDeletionTimestamp()!=null){
            log.info("[{}:{}]删除集群",spec.getNamespace(),resource.getMetadata().getName());
            return UpdateControl.noUpdate();
        }*/
        //如果校验结果无需操作，则结束
        //TODO 

        //根据CR Spec配置集群
        
        // Step 1 新建Pod-StatefulSet
        final var statefulSet = resourcesBuild.buildStatefulSet(resource);
        // Step 2 新建Headless Service
        final var headlessService = resourcesBuild.buildHeadlessService(resource);
        // Step 3 新建readService
        final var readService = resourcesBuild.buildReadService(resource);
        // Step 4 新建readwriteService
        final var writeService = resourcesBuild.buildWriteService(resource);
        log.info("spec: " + spec.toString());
        var statefulSetResource = client.resources(StatefulSet.class).withName(spec.getStatefulSetName());
        var headlessServiceResource = client.resources(Service.class).withName(spec.getHeadlessServiceName());
        var readServiceResource = client.resources(Service.class).withName(spec.getVastbaseReadServiceName());
        var writeServiceResource = client.resources(Service.class).withName(spec.getVastbaseWriteServiceName());

        // 判断这些资源是不是存在，可以用断言，代码好看一些
        var statefulSetExisting = statefulSetResource.get();
        var headlessServiceExisting = headlessServiceResource.get();
        var readServiceExisting = readServiceResource.get();
        var writeServiceExisting = writeServiceResource.get();

        // 资源不存在就创建
        CheckResult checkResult = new CheckResult();
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
        checkResult = resourcesCheck.checkStatefulSet(statefulSetExisting, statefulSet,client,resource);
        if (!checkResult.isMatch()) {
            log.info("statefulSet name of {} was created!", statefulSet.getMetadata().getName());
            statefulSetResource.createOrReplace(statefulSet);
        }
        return UpdateControl.noUpdate();
    }

    
}

