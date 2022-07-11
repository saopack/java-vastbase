package com.vastdata.cluster;


import com.vastdata.vo.CheckResult;
import io.fabric8.kubernetes.api.model.*;
import io.fabric8.kubernetes.api.model.apiextensions.v1.CustomResourceDefinition;
import io.fabric8.kubernetes.api.model.apiextensions.v1.CustomResourceDefinitionList;
import io.fabric8.kubernetes.api.model.apps.StatefulSet;
import io.fabric8.kubernetes.client.CustomResource;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.javaoperatorsdk.operator.api.reconciler.Context;
import io.javaoperatorsdk.operator.api.reconciler.Reconciler;
import io.javaoperatorsdk.operator.api.reconciler.UpdateControl;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import java.util.List;
import java.util.concurrent.TimeUnit;

public class VastbaseClusterReconciler implements Reconciler<VastbaseCluster> {
    private final KubernetesClient client;
    private static final Logger log = LoggerFactory.getLogger(VastbaseClusterReconciler.class);
    @Inject
    private SyncHandler syncHandler;

    public VastbaseClusterReconciler(KubernetesClient client) {
        this.client = client;
    }

    @Override
    public UpdateControl<VastbaseCluster> reconcile(VastbaseCluster resource, Context context) {
        var spec = resource.getSpec();
        //查询cr
        VastbaseCluster resourceEx = client.resources(VastbaseCluster.class).inNamespace(resource.getMetadata().getNamespace()).withName(resource.getMetadata().getName()).get();
        //cr处理逻辑如果CR不存在，则结束
        if (resourceEx == null) {
            return UpdateControl.noUpdate();
        }
        //如果CR已被删除，进行相应资源的清理操作
        if (resourceEx.getMetadata().getDeletionTimestamp() != null) {
            log.info("[{}:{}]删除集群", spec.getNamespace(), resource.getMetadata().getName());
            return UpdateControl.noUpdate();
        }
        //校验cr是否合法，不合法则结束
        //TODO 

        //根据CR Spec配置集群
        try {
            syncHandler.syncCluster(resource, client);
        } catch (Exception e) {
            log.error("[{}:{}]配置集群发生错误，将于{}秒后重试", spec.getNamespace(), resource.getCRDName(), 30);
            e.printStackTrace();
            try {
                Thread.sleep(60000);
            } catch (InterruptedException ie) {
                ie.printStackTrace();
            }
            return UpdateControl.<VastbaseCluster>noUpdate().rescheduleAfter(60, TimeUnit.SECONDS);
        }
        return UpdateControl.<VastbaseCluster>noUpdate().rescheduleAfter(60, TimeUnit.SECONDS);
    }
}

