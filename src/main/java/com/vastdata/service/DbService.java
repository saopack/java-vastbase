package com.vastdata.service;

import com.vastdata.constants.VbConst;
import io.fabric8.kubernetes.api.model.Pod;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.fabric8.kubernetes.client.dsl.ExecWatch;
import io.netty.util.internal.StringUtil;
import lombok.extern.slf4j.Slf4j;

import javax.enterprise.context.ApplicationScoped;
import java.io.ByteArrayOutputStream;
import java.util.List;

@Slf4j
@ApplicationScoped
public class DbService {
    public String executeVbCommand(KubernetesClient client, String namespace, String name,String exec,String action, String parameter){
        log.info("[{}:{}]执行命令: {}",namespace,name,parameter);
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        client.pods()
                .inNamespace(namespace)
                .withName(name)
                .writingOutput(baos)
                .writingError(baos)
                .exec(generateCommand(exec,action,parameter));
        return baos.toString();
    }

    public String generateCommand(String exec, String action, String params) {
        return exec + " " + action+" "+params;
    }
    public String generateDBConfigPropParam(String name, String value, boolean quote){
       if(quote){
           return name+"="+"'"+value+"'";
       } 
       return name+"="+value;
    }
    public String reload(KubernetesClient client, String namespace, String name, String parameter){
        return executeVbCommand(client,namespace,name, VbConst.VB_CFG_CMD,VbConst.CFG_PARAM_RELOAD,parameter);        
    }
    
}
