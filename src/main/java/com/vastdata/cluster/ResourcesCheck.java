package com.vastdata.cluster;

import com.vastdata.vo.CheckResult;
import io.fabric8.kubernetes.api.model.PersistentVolume;
import io.fabric8.kubernetes.api.model.PersistentVolumeClaim;
import io.fabric8.kubernetes.api.model.Secret;
import io.fabric8.kubernetes.api.model.Service;
import io.fabric8.kubernetes.api.model.apps.StatefulSet;

import javax.enterprise.context.ApplicationScoped;

@ApplicationScoped
public class ResourcesCheck {
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

    public CheckResult checkStatefulSet(StatefulSet statefulSetExisting, StatefulSet statefulSet) {
        CheckResult result = new CheckResult();
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
            match = false;
            reasons.append("statefulSet:{"+statefulSet.getMetadata().getName()+"}:replicas不匹配!");
        }   
        result.setMatch(match);
        result.setReasons(reasons.toString());
        return result;
    }
}
