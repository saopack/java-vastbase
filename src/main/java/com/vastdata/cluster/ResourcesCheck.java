package com.vastdata.cluster;

import com.vastdata.constants.VbConst;
import com.vastdata.service.DbService;
import com.vastdata.vo.CheckResult;
import io.fabric8.kubernetes.api.model.*;
import io.fabric8.kubernetes.api.model.apps.StatefulSet;
import io.fabric8.kubernetes.client.KubernetesClient;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import java.util.List;

@ApplicationScoped
public class ResourcesCheck {
    @Inject
    private DbService dbService;
    public CheckResult checkPV(PersistentVolume pvExisting, PersistentVolume persistentVolume) {
        CheckResult result = new CheckResult();
        boolean match = true;
        StringBuffer reasons = new StringBuffer();
        if(pvExisting==null){
            match = false;
            reasons.append("pv:{"+persistentVolume.getMetadata().getName()+"}不存在!");
            result.setMatch(match);
            result.setReasons(reasons.toString());
            return result;
        }
        result.setMatch(match);
        result.setReasons(reasons.toString());
        return result;
    }

    public CheckResult checkPVC(PersistentVolumeClaim pvcExisting, PersistentVolumeClaim persistentVolumeClaim) {
        CheckResult result = new CheckResult();
        boolean match = true;
        StringBuffer reasons = new StringBuffer();
        if(pvcExisting==null){
            match = false;
            reasons.append("pvc:{"+persistentVolumeClaim.getMetadata().getName()+"}不存在!");
            result.setMatch(match);
            result.setReasons(reasons.toString());
            return result;
        }
        result.setMatch(match);
        result.setReasons(reasons.toString());
        return result;
    }

    public CheckResult checkSecret(Secret secretExisting, PersistentVolumeClaim persistentVolumeClaim) {
        CheckResult result = new CheckResult();
        boolean match = true;
        StringBuffer reasons = new StringBuffer();
        if(secretExisting==null){
            match = false;
            reasons.append("Secret:{"+persistentVolumeClaim.getMetadata().getName()+"}不存在!");
            result.setMatch(match);
            result.setReasons(reasons.toString());
            return result;
        }
        result.setMatch(match);
        result.setReasons(reasons.toString());
        return result;
    }

    public CheckResult checkHeadlessService(Service headlessServiceExisting, Service headlessService) {
        CheckResult result = new CheckResult();
        boolean match = true;
        StringBuffer reasons = new StringBuffer();
        if(headlessServiceExisting==null){
            match = false;
            reasons.append("headlessService:{"+headlessService.getMetadata().getName()+"}不存在!");
            result.setMatch(match);
            result.setReasons(reasons.toString());
            return result;
        }
        result.setMatch(match);
        result.setReasons(reasons.toString());
        return result;
    }

    public CheckResult checkReadService(Service readServiceExisting, Service readService) {
        CheckResult result = new CheckResult();
        boolean match = true;
        StringBuffer reasons = new StringBuffer();
        if(readServiceExisting==null){
            match = false;
            reasons.append("readService:{"+readService.getMetadata().getName()+"}不存在!");
            result.setMatch(match);
            result.setReasons(reasons.toString());
            return result;
        }
        result.setMatch(match);
        result.setReasons(reasons.toString());
        return result;
    }

    public CheckResult checkReadWriteService(Service writeServiceExisting, Service writeService) {
        CheckResult result = new CheckResult();
        boolean match = true;
        StringBuffer reasons = new StringBuffer();
        if(writeServiceExisting==null){
            match = false;
            reasons.append("writeService:{"+writeService.getMetadata().getName()+"}不存在!");
            result.setMatch(match);
            result.setReasons(reasons.toString());
            return result;
        }
        result.setMatch(match);
        result.setReasons(reasons.toString());
        return result;
    }

    public CheckResult checkStatefulSet(StatefulSet statefulSetExisting, StatefulSet statefulSet, KubernetesClient client,VastbaseCluster resource) {
        CheckResult result = new CheckResult();
        String namespace = resource.getSpec().getNamespace();
        int port = resource.getSpec().getContainerPort();
        boolean match = true;
        StringBuffer reasons = new StringBuffer();
        if(statefulSetExisting==null){
            match = false;
            reasons.append("statefulSet:{"+statefulSet.getMetadata().getName()+"}不存在!");
            result.setMatch(match);
            result.setReasons(reasons.toString());
            return result;
        }
        if(!statefulSetExisting.getSpec().getReplicas().equals(statefulSet.getSpec().getReplicas())){
            int replicas = statefulSet.getSpec().getReplicas();
            match = false;
            reasons.append("statefulSet:{"+statefulSet.getMetadata().getName()+"}:replicas不匹配!");
            //修改数据库配置
            List<Pod> podsList = client.pods().inNamespace(namespace).list().getItems();
            for (Pod pod:podsList){
                String podName = pod.getMetadata().getName();
                if(!podName.startsWith(resource.getSpec().getContainerName())){
                    continue;
                }
                int podOrder = podName.charAt(podName.length()-1);
                int order = 0;
                String ip = pod.getStatus().getPodIP();                
                for(int i=0;i<replicas;i++){
                    if(podOrder!=i){
                        order++; 
                        String configName = String.format(VbConst.REPL_CONN_INFO_NAME,order);
                        String configValue = String.format(VbConst.REPL_CONN_INFO_VALUE,ip,port+2,port+3,podName.replace(String.valueOf(order),String.valueOf(i)),port+2,port+3);
                        String execDbCmd = dbService.generateDBConfigPropParam(configName,configValue,true);
                        dbService.reload(client,namespace,pod.getMetadata().getName(),execDbCmd);
                    }
                }
            }
        }   
        result.setMatch(match);
        result.setReasons(reasons.toString());
        return result;
    }
}
