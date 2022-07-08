package com.vastdata.service;

import com.vastdata.cluster.VastbaseCluster;
import com.vastdata.constants.PodStatus;
import com.vastdata.constants.VbConst;
import com.vastdata.utils.LSNUtil;
import com.vastdata.vo.LSN;
import io.fabric8.kubernetes.api.model.Pod;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.fabric8.kubernetes.client.dsl.ExecListener;
import lombok.extern.slf4j.Slf4j;

import javax.enterprise.context.ApplicationScoped;
import java.io.ByteArrayOutputStream;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

@Slf4j
@ApplicationScoped
public class DbService {
    public String executeVbCommand(KubernetesClient client, String namespace, String name, String... exec) {
        log.info("[{}:{}]执行命令: {}", namespace, name, exec);
        CompletableFuture<String> data = new CompletableFuture<>();
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        String result = "";
        try {
            client.pods()
                    .inNamespace(namespace)
                    .withName(name)
                    .inContainer("vastbase")
                    .writingOutput(baos)
                    .writingError(baos)
                    .usingListener(new SimpleListener(data, baos))
                    .exec(exec);
            result = data.get(10, TimeUnit.SECONDS);
            log.info("[{}:{}]执行命令:返回结果:{}", namespace, name, result);
        } catch (Exception e) {
            log.error("命令执行失败:{}", e.getMessage());
        }
        return result;
    }

    public String addMaintainFlag(KubernetesClient client, String namespace, String name, String parameter) {
        return executeVbCommand(client, namespace, name, VbConst.ENABLE_MAINTENANCE_CMD.split(" "));
    }

    public String delMaintainFlag(KubernetesClient client, String namespace, String name, String parameter) {
        return executeVbCommand(client, namespace, name, VbConst.DISABLE_MAINTENANCE_CMD.split(" "));
    }

    /**
     * 查询节点主备状态 f为主t为备
     */
    public boolean checkDBRepl(KubernetesClient client, String namespace, String name) {
        String result = executeVbCommand(client, namespace, name, VbConst.VB_QL_CMD, VbConst.CFG_PARAM_C, VbConst.VB_SQL_CHECKPRIMARY);
        if (result.contains("f")) {
            return true;
        } else if (result.contains("t")) {
            return false;
        }
        return false;
    }

    public Pod findPodWithLargestLSN(KubernetesClient client, List<Pod> podList, VastbaseCluster resource) {
        LSN maxLsn = new LSN();
        for (Pod pod : podList) {
            LSN lsn = GetDBLSN(client, pod, resource);
            int result = LSNUtil.getMaxLSNPod(maxLsn,lsn);
            if(result<0){
                maxLsn = lsn;
            }
        }
        Pod pod = client.pods().inNamespace(resource.getSpec().getNamespace()).withName(maxLsn.getPodName()).get();
        return pod;
    }



    private LSN GetDBLSN(KubernetesClient client, Pod pod, VastbaseCluster resource) {
        String LSNStr = "";
        if (checkDBRepl(client, resource.getSpec().getNamespace(), pod.getMetadata().getName())) {
            LSNStr = executeVbCommand(client, resource.getSpec().getNamespace(), pod.getMetadata().getName(), VbConst.VB_QL_CMD, VbConst.CFG_PARAM_C, VbConst.VB_SQL_LSN_PRIMARY);

        } else {
            LSNStr = executeVbCommand(client, resource.getSpec().getNamespace(), pod.getMetadata().getName(), VbConst.VB_QL_CMD, VbConst.CFG_PARAM_C, VbConst.VB_SQL_LSN_STANDBY);
        }
        return LSNUtil.parseLSN(pod.getMetadata().getName(),LSNStr);
    }

    public boolean ConfigDB(KubernetesClient client, VastbaseCluster resource, List<Pod> podList, Pod podPrimary) {
        restartPrimary(client, resource, podPrimary);
        podList.remove(podPrimary);
        for(Pod pod: podList){
            restartStandby(client, resource, pod);
        }
        return true;
    }

    private void restartStandby(KubernetesClient client, VastbaseCluster resource, Pod podStandby) {
        boolean check = checkFirstJoin(client,resource.getSpec().getNamespace(), podStandby.getMetadata().getName());
        if(check){
            standbyInit(client, resource.getSpec().getNamespace(), podStandby.getMetadata().getName());
        }
        restart(client, resource.getSpec().getNamespace(), podStandby.getMetadata().getName(), VbConst.CTL_M_STANDBY);
    }

    private void restartPrimary(KubernetesClient client, VastbaseCluster resource, Pod podPrimary) {
        restart(client, resource.getSpec().getNamespace(), podPrimary.getMetadata().getName(), VbConst.CTL_M_PRIMARY);
    }

    public boolean checkDBState(KubernetesClient client, VastbaseCluster resource, Pod pod) {
        if (!pod.getStatus().getPhase().equals(PodStatus.PodRunning.getPodStatus())) {
            String podName = pod.getMetadata().getName();
            log.info("[{}:{}]Pod {}未处于运行阶段", resource.getSpec().getNamespace(), resource.getSpec().getContainerName(), podName);
            return false;
        }
        return true;

    }

    public void switchPrimary(KubernetesClient client, VastbaseCluster resource, Pod newPrimary, Pod originPrimary) {
        switchover(client,resource.getSpec().getNamespace(),newPrimary.getMetadata().getName());
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
        String config = String.format(VbConst.DB_CONFIG_PARAM, name, value);
        return config;
    }

    public String reload(KubernetesClient client, String namespace, String name, String parameter) {
        return executeVbCommand(client, namespace, name, VbConst.VB_CFG_CMD, VbConst.DB_DATA_CMD, VbConst.DB_DATA, VbConst.CFG_PARAM_RELOAD, VbConst.CFG_PARAM_C, parameter);
    }

    private String restart(KubernetesClient client, String namespace, String name, String ctlMStandby) {
        return executeVbCommand(client, namespace, name, VbConst.VB_CTL_CMD, VbConst.CTL_PARAM_RESTART, VbConst.CFG_PARAM_M, ctlMStandby);
    }

    private String switchover(KubernetesClient client, String namespace, String name) {
        return executeVbCommand(client, namespace, name, VbConst.VB_CTL_CMD, VbConst.CTL_PARAM_SWITCHOVER);
    }

    private String standbyInit(KubernetesClient client, String namespace, String name) {
        return executeVbCommand(client,namespace,name,VbConst.VB_CTL_CMD,VbConst.CTL_PARAM_BUILD,VbConst.CFG_PARAM_B,VbConst.CTL_PARAM_FULL);
    }

    private boolean checkFirstJoin(KubernetesClient client, String namespace, String name) {
        String postgresql_conf_bk_ex = executeVbCommand(client,namespace,name,"sh","-c","if [ -f '/home/vastbase/vbdata/postgresql.conf.bak' ]; then  echo 0; else echo 1; fi");
        if(postgresql_conf_bk_ex.contains("0")){
            return false;
        }
        return true;
    }


}
