package com.vastdata.cluster;

import com.vastdata.constants.AccessModeEnum;
import com.vastdata.constants.ApiVersionEnum;
import com.vastdata.constants.ImagePullPolicEnum;
import com.vastdata.constants.ResourceKindEnum;
import io.fabric8.kubernetes.api.model.*;
import io.fabric8.kubernetes.api.model.apps.StatefulSet;
import io.fabric8.kubernetes.api.model.apps.StatefulSetSpec;

import javax.enterprise.context.ApplicationScoped;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@ApplicationScoped
public class ResourcesBuild {
    public Service buildReadService(VastbaseCluster vastbaseReplica) {
        var vastbaseClusterSpec = vastbaseReplica.getSpec();
        var labels = new HashMap<String, String>();
        labels.put("origin", vastbaseClusterSpec.getContainerName());
        ObjectMeta objectMeta = new ObjectMeta();
        objectMeta.setName(vastbaseClusterSpec.getVastbaseReadServiceName());
        objectMeta.setLabels(labels);

        var servicePort = new ServicePort();
        servicePort.setName("vsql");
        servicePort.setPort(vastbaseClusterSpec.getContainerPort());
        servicePort.setNodePort(vastbaseClusterSpec.getVastbaseServiceReadNodePort());

        var servicePorts = new ArrayList<ServicePort>();
        servicePorts.add(servicePort);

        var serviceSpec = new ServiceSpec();
        serviceSpec.setSelector(labels);
        serviceSpec.setPorts(servicePorts);
        serviceSpec.setType("NodePort");

        var service = new Service();
        service.setApiVersion(ApiVersionEnum.V1.getApiVersion());
        service.setKind(ResourceKindEnum.SERVICE.getKind());
        service.setMetadata(objectMeta);
        service.setSpec(serviceSpec);

        return service;
    }

    public Service buildWriteService(VastbaseCluster vastbaseReplica) {
        var vastbaseClusterSpec = vastbaseReplica.getSpec();
        var labels = new HashMap<String, String>();
        //序号0的副本作为主库提供读写能力
        labels.put("origin", vastbaseClusterSpec.getContainerName());
        labels.put("role", "master");
        ObjectMeta objectMeta = new ObjectMeta();
        objectMeta.setName(vastbaseClusterSpec.getVastbaseWriteServiceName());
        objectMeta.setLabels(labels);

        var servicePort = new ServicePort();
        servicePort.setName("vsql");
        servicePort.setPort(vastbaseClusterSpec.getContainerPort());
        servicePort.setNodePort(vastbaseClusterSpec.getVastbaseServiceWriteNodePort());

        var servicePorts = new ArrayList<ServicePort>();
        servicePorts.add(servicePort);

        var serviceSpec = new ServiceSpec();
        serviceSpec.setSelector(labels);
        serviceSpec.setPorts(servicePorts);
        serviceSpec.setType("NodePort");

        var service = new Service();
        service.setApiVersion(ApiVersionEnum.V1.getApiVersion());
        service.setKind(ResourceKindEnum.SERVICE.getKind());
        service.setMetadata(objectMeta);
        service.setSpec(serviceSpec);

        return service;
    }

    public Service buildHeadlessService(VastbaseCluster vastbaseReplica, String podName) {
        var vastbaseClusterSpec = vastbaseReplica.getSpec();

        // metadata部分
        Map<String, String> labels = new HashMap<>();
        labels.put("app", podName);
        var objectMeta = new ObjectMeta();
        objectMeta.setName(podName);
        objectMeta.setLabels(labels);

        // spec.ports
        var servicePort = new ServicePort();
        servicePort.setName("vsql");
        servicePort.setPort(vastbaseClusterSpec.getContainerPort());
        var innerPort = new ServicePort();
        innerPort.setName("innerport");
        innerPort.setPort(vastbaseClusterSpec.getContainerPort() + 1);
        var localPort = new ServicePort();
        localPort.setName("localport");
        localPort.setPort(vastbaseClusterSpec.getContainerPort() + 2);
        var localService = new ServicePort();
        localService.setName("localservice");
        localService.setPort(vastbaseClusterSpec.getContainerPort() + 3);
        var servicePorts = new ArrayList<ServicePort>();
        servicePorts.add(servicePort);
        servicePorts.add(innerPort);
        servicePorts.add(localPort);
        servicePorts.add(localService);

        // spec.selector
        var label = new HashMap<String, String>();
        label.put("app", podName);

        // spec
        var serviceSpec = new ServiceSpec();
        serviceSpec.setSelector(label);
        serviceSpec.setPorts(servicePorts);
        serviceSpec.setClusterIP("None");

        var service = new Service();
        service.setApiVersion(ApiVersionEnum.V1.getApiVersion());
        service.setKind(ResourceKindEnum.SERVICE.getKind());
        service.setMetadata(objectMeta);
        service.setSpec(serviceSpec);

        return service;
    }

    public StatefulSet buildStatefulSet(VastbaseCluster vastbaseReplica) {
        VastbaseClusterSpec vbSpec = vastbaseReplica.getSpec();

        // 元信息。主要是statefulSet的名称
        var objectMeta = new ObjectMeta();
        objectMeta.setName(vbSpec.getStatefulSetName());

        // spec.selector信息
        Map<String, String> label = new HashMap<>();
        label.put("app", vbSpec.getContainerName());
        var labelSelector = new LabelSelector();
        labelSelector.setMatchLabels(label);

        // spec.template的信息
        var podMeta = new ObjectMeta();
        podMeta.setLabels(label);

        // spec.container.resource信息
/*
        Map<String, Quantity> memLimits = new HashMap<>();
        Map<String, Quantity> cpuLimits = new HashMap<>();
        memLimits.put("memory", new Quantity("256Mi"));
        cpuLimits.put("cpu", new Quantity("500m"));
        var resourceRequirements = new ResourceRequirements();
        resourceRequirements.setLimits(memLimits);
        resourceRequirements.setLimits(cpuLimits);
*/

        // spec.container.ports信息
        var containerPort = new ContainerPort();
        containerPort.setContainerPort(vbSpec.getContainerPort());
        var containerPorts = new ArrayList<ContainerPort>();
        containerPorts.add(containerPort);

        // spec.container.volumeMounts信息
        var volumeMounts = new ArrayList<VolumeMount>();

        // 数据目录的挂载
        var storageVolumeMount = new VolumeMount();
        storageVolumeMount.setName(vbSpec.getVastbasePersistentStorageMountName());
        storageVolumeMount.setMountPath(vbSpec.getVastbasePersistentStorageMountPath());

        // 配置文件的挂载
        var cnfVolumeMount = new VolumeMount();
        cnfVolumeMount.setName(vbSpec.getVastbaseCnfMountName());
        cnfVolumeMount.setMountPath(vbSpec.getVastbaseCnfMountPath());
        cnfVolumeMount.setSubPath(vbSpec.getVastbaseCnfMountSubPath());

        volumeMounts.add(storageVolumeMount);
        //volumeMounts.add(cnfVolumeMount);
        // spec .container.env信息
        var envs = new ArrayList<EnvVar>();
        envs.add(new EnvVar("REPLICAS", vbSpec.getReplicas().toString(), null));
        envs.add(new EnvVar("PORT", vbSpec.getContainerPort().toString(), null));
        envs.add(new EnvVar("SCOPE_NAME", vbSpec.getStatefulSetName(), null));
        envs.add(new EnvVar("NAMESPACE", vbSpec.getNamespace(), null));
        envs.add(new EnvVar("SERVICE_NAME", vbSpec.getHeadlessServiceName(), null));

        var envVarSourceIP = new EnvVarSource();
        var fieldSelector = new ObjectFieldSelector();
        fieldSelector.setFieldPath("status.podIP");
        envVarSourceIP.setFieldRef(fieldSelector);
        envs.add(new EnvVar("HOST_IP", null, envVarSourceIP));

        var envVarSourceVasePwd = new EnvVarSource();
        envVarSourceVasePwd.setFieldRef(null);
        var secretKeySelector = new SecretKeySelector("vastbase_password", vbSpec.getSecretName(), null);
        envVarSourceVasePwd.setSecretKeyRef(secretKeySelector);
        envs.add(new EnvVar("VASTBASE_PASSWORD", null, envVarSourceVasePwd));

        var envVarSourceVbaPwd = new EnvVarSource();
        var secretKeySelectorVbaPwd = new SecretKeySelector("vbadmin_password", vbSpec.getSecretName(), null);
        envVarSourceVbaPwd.setSecretKeyRef(secretKeySelectorVbaPwd);
        envs.add(new EnvVar("VBADMIN_PASSWORD", null, envVarSourceVbaPwd));
        // spec.container信息
        var container = new Container();
        container.setName(vbSpec.getContainerName());
        container.setImage(vbSpec.getImage());
        container.setImagePullPolicy(ImagePullPolicEnum.A.getPolic());
        container.setEnv(envs);
        //TODO 未确定具体数值 暂不做限制--yangjie
        //container.setResources(resourceRequirements);
        container.setPorts(containerPorts);
        container.setVolumeMounts(volumeMounts);

        var containers = new ArrayList<Container>();
        containers.add(container);

        // spec.volumes信息
        // PVC的挂载
        var persistentVolumeClaimVolumeSource = new PersistentVolumeClaimVolumeSource();
        persistentVolumeClaimVolumeSource.setClaimName(vbSpec.getPvcName());
        var pvcVolume = new Volume();
        pvcVolume.setName(vbSpec.getVastbasePersistentStorageMountName());
        pvcVolume.setPersistentVolumeClaim(persistentVolumeClaimVolumeSource);
        var volumes = new ArrayList<Volume>();
        volumes.add(pvcVolume);
        // spec.volumeClaimTemplates信息 自动创建pvc
        List<PersistentVolumeClaim> volumeClaimTemplates = new ArrayList<>();
        PersistentVolumeClaim pvc = new PersistentVolumeClaim();
        ObjectMeta metadata = new ObjectMeta();
        metadata.setName(vbSpec.getVastbasePersistentStorageMountName());
        PersistentVolumeClaimSpec pcvSpec = new PersistentVolumeClaimSpec();
        List<String> accessModes = new ArrayList<>();
        accessModes.add(AccessModeEnum.RWO.getAccessMode());
        pcvSpec.setAccessModes(accessModes);
        ResourceRequirements resources = new ResourceRequirements();
        Map<String, Quantity> requests = new HashMap<>();
        requests.put("storage", new Quantity("2Gi"));
        resources.setRequests(requests);
        pcvSpec.setResources(resources);
        pvc.setMetadata(metadata);
        pvc.setSpec(pcvSpec);
        volumeClaimTemplates.add(pvc);
        // template.spec的配置
        var podSpec = new PodSpec();
        podSpec.setContainers(containers);
        podSpec.setServiceAccountName(vbSpec.getServiceAccountName());
        podSpec.setVolumes(volumes);

        // template.spec.containers.livenessProbe的配置
        var livenessProbe = new Probe();
        ExecAction exec = new ExecAction();
        List<String> command = new ArrayList<>();
        command.add("vsql");
        command.add("-c");
        command.add("SELECT 1");
        exec.setCommand(command);
        livenessProbe.setExec(exec);
        livenessProbe.setInitialDelaySeconds(30);
        livenessProbe.setPeriodSeconds(10);
        livenessProbe.setTimeoutSeconds(5);
        container.setLivenessProbe(livenessProbe);

        // template.spec.containers.readinessProbe的配置
        var readinessProbe = new Probe();
        readinessProbe.setExec(exec);
        readinessProbe.setInitialDelaySeconds(30);
        readinessProbe.setPeriodSeconds(10);
        readinessProbe.setTimeoutSeconds(5);
        container.setReadinessProbe(readinessProbe);

        // template配置
        var podTemplate = new PodTemplateSpec();
        podTemplate.setMetadata(podMeta);
        podTemplate.setSpec(podSpec);

        // spec配置
        var spec = new StatefulSetSpec();
        spec.setReplicas(vbSpec.getReplicas());
        spec.setServiceName(vbSpec.getHeadlessServiceName());
        spec.setSelector(labelSelector);
        spec.setTemplate(podTemplate);
        spec.setVolumeClaimTemplates(volumeClaimTemplates);

        var statefulSet = new StatefulSet();
        statefulSet.setApiVersion(ApiVersionEnum.APPS_V1.getApiVersion());
        statefulSet.setKind(ResourceKindEnum.STATEFUL_SET.getKind());
        statefulSet.setMetadata(objectMeta);
        statefulSet.setSpec(spec);

        return statefulSet;
    }

    public ConfigMap buildConfigMap(VastbaseCluster vastbaseReplica) {
        VastbaseClusterSpec vastbaseClusterSpec = vastbaseReplica.getSpec();

        Map<String, String> labels = new HashMap<>();
        //labels.put("app", vastbaseClusterSpec.getConfigMapName());
        var objectMeta = new ObjectMeta();
        //objectMeta.setName(vastbaseClusterSpec.getConfigMapName());
        objectMeta.setLabels(labels);

        //Map<String, String> configData = vastbaseClusterSpec.getConfigData();

        var configMap = new ConfigMap();
        configMap.setApiVersion(ApiVersionEnum.V1.getApiVersion());
        configMap.setMetadata(objectMeta);
        configMap.setKind(ResourceKindEnum.CONFIG_MAP.getKind());
        //configMap.setData(configData);

        return configMap;
    }

    public PersistentVolumeClaim buildPersistentVolumeClaim(VastbaseCluster vastbaseReplica, String podName, String volType) {
        var vastbaseClusterSpec = vastbaseReplica.getSpec();

        // resource定义部分
        var resourceRequirements = new ResourceRequirements();
        var quantity = new Quantity();
        quantity.setAmount(vastbaseClusterSpec.getPvcStorageSize());
        Map<String, Quantity> capacity = new HashMap<>();
        capacity.put("storage", quantity);
        resourceRequirements.setRequests(capacity);

        // accessMode部分
        List<String> accessModes = new ArrayList<>();
        accessModes.add(AccessModeEnum.RWO.getAccessMode());

        // spec部分
        LabelSelector labelSelector = new LabelSelector();
        Map<String, String> matchLabels = new HashMap<>();
        matchLabels.put("pv", podName + "-" + volType);
        labelSelector.setMatchLabels(matchLabels);
        var spec = new PersistentVolumeClaimSpec();
        spec.setStorageClassName(vastbaseClusterSpec.getStorageClassName());
        spec.setAccessModes(accessModes);
        spec.setResources(resourceRequirements);
        spec.setSelector(labelSelector);

        // meta部分
        Map<String, String> labels = new HashMap<>();
        labels.put("app.kubernetes.io/app", "vastbase");
        labels.put("pvc", podName + "-" + volType);
        var objectMeta = new ObjectMeta();
        objectMeta.setName(podName + "-" + volType + "-pvc");
        objectMeta.setNamespace(vastbaseClusterSpec.getNamespace());
        objectMeta.setLabels(labels);

        var persistentVolumeClaim = new PersistentVolumeClaim();
        persistentVolumeClaim.setApiVersion(ApiVersionEnum.V1.getApiVersion());
        persistentVolumeClaim.setKind(ResourceKindEnum.PVC.getKind());
        persistentVolumeClaim.setMetadata(objectMeta);
        persistentVolumeClaim.setSpec(spec);

        return persistentVolumeClaim;
    }

    public PersistentVolume buildPersistentVolume(VastbaseCluster replica, String podName, String volType) {
        var vastbaseCluster = replica.getSpec();

        // 容量信息
        var quantity = new Quantity();
        quantity.setAmount(vastbaseCluster.getPvcStorageSize());
        Map<String, Quantity> capacity = new HashMap<>();
        capacity.put("storage", quantity);

        // 访问方式
        List<String> accessModes = new ArrayList<>();
        accessModes.add(AccessModeEnum.RWO.getAccessMode());

        // 路径信息
        var hostPathVolumeSource = new HostPathVolumeSource();
        hostPathVolumeSource.setPath(vastbaseCluster.getHostPath().get("dataPath") + "/" + podName);

        // PV的spec信息
        var spec = new PersistentVolumeSpec();
        spec.setStorageClassName(vastbaseCluster.getStorageClassName());
        spec.setHostPath(hostPathVolumeSource);
        spec.setAccessModes(accessModes);
        spec.setCapacity(capacity);

        Map<String, String> labels = new HashMap<>();
        labels.put("app.kubernetes.io/app", "vastbase");
        labels.put("pv", podName + "-" + volType);
        var objectMeta = new ObjectMeta();
        objectMeta.setName(podName + "-" + volType + "-pv");
        objectMeta.setLabels(labels);
        // PV声明头
        var persistentVolume = new PersistentVolume();
        persistentVolume.setApiVersion(ApiVersionEnum.V1.getApiVersion());
        persistentVolume.setKind(ResourceKindEnum.PV.getKind());
        persistentVolume.setSpec(spec);
        persistentVolume.setMetadata(objectMeta);

        return persistentVolume;
    }

    public Pod buildPod(VastbaseCluster resource, String podName) {
        var vbSpec = resource.getSpec();
        // 元信息。主要是pod的名称
        var objectMeta = new ObjectMeta();
        objectMeta.setName(podName);
        objectMeta.setNamespace(vbSpec.getNamespace());
        // spec.selector信息
        Map<String, String> label = new HashMap<>();
        label.put("app", podName);
        label.put("origin", vbSpec.getContainerName());
        objectMeta.setLabels(label);
        // spec.container.resource信息
/*
        Map<String, Quantity> memLimits = new HashMap<>();
        Map<String, Quantity> cpuLimits = new HashMap<>();
        memLimits.put("memory", new Quantity("256Mi"));
        cpuLimits.put("cpu", new Quantity("500m"));
        var resourceRequirements = new ResourceRequirements();
        resourceRequirements.setLimits(memLimits);
        resourceRequirements.setLimits(cpuLimits);
*/

        // spec.container.ports信息
        var containerPort = new ContainerPort();
        containerPort.setContainerPort(vbSpec.getContainerPort());
        var containerPorts = new ArrayList<ContainerPort>();
        containerPorts.add(containerPort);

        // spec.container.volumeMounts信息
        var volumeMounts = new ArrayList<VolumeMount>();

        // 数据目录的挂载
        var storageVolumeMount = new VolumeMount();
        storageVolumeMount.setName(podName + "-data-pvc");
        storageVolumeMount.setMountPath(vbSpec.getVastbasePersistentStorageMountPath());


        volumeMounts.add(storageVolumeMount);
        // spec .container.env信息
        var envs = new ArrayList<EnvVar>();
        envs.add(new EnvVar("REPLICAS", vbSpec.getReplicas().toString(), null));
        envs.add(new EnvVar("PORT", vbSpec.getContainerPort().toString(), null));
        envs.add(new EnvVar("SCOPE_NAME", vbSpec.getStatefulSetName(), null));
        envs.add(new EnvVar("NAMESPACE", vbSpec.getNamespace(), null));
        envs.add(new EnvVar("SERVICE_NAME", vbSpec.getHeadlessServiceName(), null));

        var envVarSourceIP = new EnvVarSource();
        var fieldSelector = new ObjectFieldSelector();
        fieldSelector.setFieldPath("status.podIP");
        envVarSourceIP.setFieldRef(fieldSelector);
        envs.add(new EnvVar("HOST_IP", null, envVarSourceIP));

        var envVarSourceVasePwd = new EnvVarSource();
        envVarSourceVasePwd.setFieldRef(null);
        var secretKeySelector = new SecretKeySelector("vastbase_password", vbSpec.getSecretName(), null);
        envVarSourceVasePwd.setSecretKeyRef(secretKeySelector);
        envs.add(new EnvVar("VASTBASE_PASSWORD", null, envVarSourceVasePwd));

        var envVarSourceVbaPwd = new EnvVarSource();
        var secretKeySelectorVbaPwd = new SecretKeySelector("vbadmin_password", vbSpec.getSecretName(), null);
        envVarSourceVbaPwd.setSecretKeyRef(secretKeySelectorVbaPwd);
        envs.add(new EnvVar("VBADMIN_PASSWORD", null, envVarSourceVbaPwd));
        // spec.container信息
        var container = new Container();
        container.setName(vbSpec.getContainerName());
        container.setImage(vbSpec.getImage());
        container.setImagePullPolicy(ImagePullPolicEnum.A.getPolic());
        container.setEnv(envs);
        //TODO 未确定具体数值 暂不做限制--yangjie
        //container.setResources(resourceRequirements);
        container.setPorts(containerPorts);
        container.setVolumeMounts(volumeMounts);

        // spec.initcontainer.command信息
        List<String> cmdList = new ArrayList<>();
        cmdList.add("chown");
        cmdList.add("-R");
        cmdList.add("1000:1000");
        cmdList.add("/home/vastbase");
        // spec.initcontainer信息
        var initContainer = new Container();
        initContainer.setName("fix-permissions");
        initContainer.setImage("busybox");
        initContainer.setCommand(cmdList);
        initContainer.setVolumeMounts(volumeMounts);

        var containers = new ArrayList<Container>();
        containers.add(container);

        var pvcVolume = new Volume();
        var persistentVolumeClaimVolumeSource = new PersistentVolumeClaimVolumeSource();
        persistentVolumeClaimVolumeSource.setClaimName(podName + "-data-pvc");
        pvcVolume.setName(podName + "-data-pvc");
        pvcVolume.setPersistentVolumeClaim(persistentVolumeClaimVolumeSource);
        var volumes = new ArrayList<Volume>();
        volumes.add(pvcVolume);

        // template.spec的配置
        var podSpec = new PodSpec();
        podSpec.setContainers(containers);
        podSpec.setInitContainers(new ArrayList<>() {{
            add(initContainer);
        }});
        podSpec.setVolumes(volumes);
        podSpec.setServiceAccountName(vbSpec.getServiceAccountName());
        // spec.containers.livenessProbe的配置
        var livenessProbe = new Probe();
        ExecAction exec = new ExecAction();
        List<String> command = new ArrayList<>();
        command.add("vsql");
        command.add("-c");
        command.add("SELECT 1");
        exec.setCommand(command);
        livenessProbe.setExec(exec);
        livenessProbe.setInitialDelaySeconds(30);
        livenessProbe.setPeriodSeconds(10);
        livenessProbe.setTimeoutSeconds(5);
        container.setLivenessProbe(livenessProbe);

        // spec.containers.readinessProbe的配置
        var readinessProbe = new Probe();
        readinessProbe.setExec(exec);
        readinessProbe.setInitialDelaySeconds(60);
        readinessProbe.setPeriodSeconds(10);
        readinessProbe.setTimeoutSeconds(5);
        container.setReadinessProbe(readinessProbe);

        var pod = new Pod();
        pod.setApiVersion(ApiVersionEnum.V1.getApiVersion());
        pod.setKind(ResourceKindEnum.POD.getKind());
        pod.setMetadata(objectMeta);
        pod.setSpec(podSpec);
        return pod;
    }
}
