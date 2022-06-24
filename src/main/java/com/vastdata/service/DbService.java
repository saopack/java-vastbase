package com.vastdata.service;

import com.vastdata.constants.VbConst;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.fabric8.kubernetes.client.dsl.ExecListener;
import lombok.extern.slf4j.Slf4j;

import javax.enterprise.context.ApplicationScoped;
import java.io.ByteArrayOutputStream;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

@Slf4j
@ApplicationScoped
public class DbService {
    public String executeVbCommand(KubernetesClient client, String namespace, String name, String... exec) {
        log.info("[{}:{}]执行命令: {}", namespace, name, exec);
        CompletableFuture<String> data = new CompletableFuture<>();
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        try {
            client.pods()
                    .inNamespace(namespace)
                    .withName(name)
                    .writingOutput(baos)
                    .writingError(baos)
                    .usingListener(new SimpleListener(data, baos))
                    .exec(exec);
            log.info("[{}:{}]执行命令:返回结果:{}", namespace, name,  data.get(10, TimeUnit.SECONDS));
        }catch (Exception e){
            log.error("命令执行失败:{}",e.getMessage());
        }
        return baos.toString();
    }

    static class SimpleListener implements ExecListener {

        private CompletableFuture<String> data;
        private ByteArrayOutputStream baos;

        public SimpleListener(CompletableFuture<String> data, ByteArrayOutputStream baos) {
            this.data = data;
            this.baos = baos;
        }

        @Override
        public void onOpen() {
            System.out.println("Reading data... ");
        }

        @Override
        public void onFailure(Throwable t, Response failureResponse) {
            System.err.println(t.getMessage());
            data.completeExceptionally(t);
        }

        @Override
        public void onClose(int code, String reason) {
            System.out.println("Exit with: " + code + " and with reason: " + reason);
            data.complete(baos.toString());
        }
    }

    public String generateDBConfigPropParam(String name, String value, boolean quote) {
        if (quote) {
             value = "'" + value + "'";
        }
        String config = String.format(VbConst.DB_CONFIG_PARAM,name,value);
        return config;
    }

    public String reload(KubernetesClient client, String namespace, String name, String parameter){
        return executeVbCommand(client,namespace,name,VbConst.VB_CFG_CMD,VbConst.DB_DATA_CMD, VbConst.DB_DATA,VbConst.CFG_PARAM_RELOAD,VbConst.CFG_PARAM_RELOAD_WITH_PARAM,parameter);
    }

}
