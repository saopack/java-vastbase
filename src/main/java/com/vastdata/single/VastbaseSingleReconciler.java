package com.vastdata.single;

import com.vastdata.constants.AccessModeEnum;
import com.vastdata.constants.ApiVersionEnum;
import com.vastdata.constants.ResourceKindEnum;
import io.fabric8.kubernetes.api.model.*;
import io.fabric8.kubernetes.api.model.apps.StatefulSet;
import io.fabric8.kubernetes.api.model.apps.StatefulSetSpec;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.javaoperatorsdk.operator.api.reconciler.Context;
import io.javaoperatorsdk.operator.api.reconciler.Reconciler;
import io.javaoperatorsdk.operator.api.reconciler.UpdateControl;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class VastbaseSingleReconciler implements Reconciler<VastbaseSingle> { 
  private final KubernetesClient client;
  private static final Logger log = LoggerFactory.getLogger(VastbaseSingleReconciler.class);

  public VastbaseSingleReconciler(KubernetesClient client) {
    this.client = client;
  }

  // TODO Fill in the rest of the reconciler

  @Override
  public UpdateControl<VastbaseSingle> reconcile(VastbaseSingle resource, Context context) {
    // TODO: fill in logic
    var spec = resource.getSpec();
    // TODO: fill in logic
    //var secretResource = client.resources(Secret.class).withName(resource.getSpec().getSecretName());
    // Step 1 新建PV
    final var persistentVolume = buildPersistentVolume(resource);
    // Step 2 新建PVC
    final var persistentVolumeClaim = buildPersistentVolumeClaim(resource);
    // Step 3 新建configMap
    final var configMap = buildConfigMap(resource);
    // Step 4 新建Pod-StatefulSet
    final var statefulSet = buildStatefulSet(resource);
    // Step 5 新建Headless Service
    final var headlessService = buildHeadlessService(resource);
    // Step 6 新建Service
    final var service = buildService(resource);
    log.info("spec: " + spec.toString());
    System.out.println();
    var pvResource = client.resources(PersistentVolume.class).withName(spec.getPvName());
    var pvcResource = client.resources(PersistentVolumeClaim.class).withName(spec.getPvcName());
    //var configMapResource = client.resources(ConfigMap.class).withName(spec.getConfigMapName());
    var statefulSetResource = client.resources(StatefulSet.class).withName(spec.getStatefulSetName());
    var headlessServiceResource = client.resources(Service.class).withName(spec.getHeadlessServiceName());
    var serviceResource = client.resources(Service.class).withName(spec.getVastbaseServiceName());

    // 判断这些资源是不是存在，可以用断言，代码好看一些
    var pvcExisting = pvcResource.get();
    var pvExisting = pvResource.get();
    //var configMapExisting = configMapResource.get();
    var statefulSetExisting = statefulSetResource.get();
    var headlessServiceExisting = headlessServiceResource.get();
    var serviceExisting = serviceResource.get();

    // 资源不存在就创建
    if (pvExisting == null) {
      log.info("pv name of {} was created!", persistentVolume.getMetadata().getName());
      pvResource.createOrReplace(persistentVolume);
    }
    if (pvcExisting == null) {
      log.info("pvc name of {} was created!", persistentVolumeClaim.getMetadata().getName());
      pvcResource.createOrReplace(persistentVolumeClaim);
    }
    //if (configMapExisting == null) {
    //  LOGGER.info("configMap name of {} was created!", configMap.getMetadata().getName());
    //  configMapResource.createOrReplace(configMap);
    //}
    if (statefulSetExisting == null) {
      log.info("statefulset named of {} was created!", statefulSet.getMetadata().getName());
      statefulSetResource.createOrReplace(statefulSet);
    }
    if (headlessServiceExisting == null) {
      log.info("headless Service named of {} was created!", headlessService.getMetadata().getName());
      headlessServiceResource.createOrReplace(headlessService);
    }
    if (serviceExisting == null) {
      log.info("Service named of {} was created!", service.getMetadata().getName());
      serviceResource.createOrReplace(service);
    }

    return UpdateControl.noUpdate();
  }

  private Service buildService(VastbaseSingle vastbaseReplica) {
    var vastbaseSingleSpec = vastbaseReplica.getSpec();

    //TODO 这里写死的，以后要改
    var labels = new HashMap<String, String>();
    labels.put("app", "vastbase");
    ObjectMeta objectMeta = new ObjectMeta();
    objectMeta.setName(vastbaseSingleSpec.getVastbaseServiceName());
    objectMeta.setLabels(labels);

    var servicePort = new ServicePort();
    servicePort.setName(vastbaseSingleSpec.getVastbaseServiceName());
    servicePort.setPort(vastbaseSingleSpec.getContainerPort());
    servicePort.setNodePort(vastbaseSingleSpec.getVastbaseServiceNodePort());

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

  private Service buildHeadlessService(VastbaseSingle vastbaseReplica) {
    var vastbaseSingleSpec = vastbaseReplica.getSpec();

    // metadata部分
    Map<String, String> labels = new HashMap<>();
    //TODO 这里还是写死的，尝试对其进行修改
    labels.put("app", "vastbase");
    var objectMeta = new ObjectMeta();
    objectMeta.setName(vastbaseSingleSpec.getHeadlessServiceName());
    objectMeta.setLabels(labels);

    // spec.ports
    var servicePort = new ServicePort();
    servicePort.setName("vastbase");
    servicePort.setPort(5432);
    var servicePorts = new ArrayList<ServicePort>();
    servicePorts.add(servicePort);

    // spec.selector
    var label = new HashMap<String, String>();
    label.put("app", "vastbase");

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

  private StatefulSet buildStatefulSet(VastbaseSingle vastbaseReplica) {
    VastbaseSingleSpec vastbaseSingleSpec = vastbaseReplica.getSpec();

    // 元信息。主要是statefulSet的名称
    var objectMeta = new ObjectMeta();
    objectMeta.setName(vastbaseSingleSpec.getStatefulSetName());

    // spec.selector信息
    Map<String, String> label = new HashMap<>();
    label.put("app", "vastbase");
    var labelSelector = new LabelSelector();
    labelSelector.setMatchLabels(label);

    // spec.template的信息
    var podMeta = new ObjectMeta();
    podMeta.setLabels(label);

    // spec.container.resource信息
    Map<String, Quantity> memLimits = new HashMap<>();
    Map<String, Quantity> cpuLimits = new HashMap<>();
    memLimits.put("memory", new Quantity("256Mi"));
    cpuLimits.put("cpu", new Quantity("500m"));
    var resourceRequirements = new ResourceRequirements();
    resourceRequirements.setLimits(memLimits);
    resourceRequirements.setLimits(cpuLimits);

    // spec.container.ports信息
    var containerPort = new ContainerPort();
    containerPort.setContainerPort(vastbaseSingleSpec.getContainerPort());
    var containerPorts = new ArrayList<ContainerPort>();
    containerPorts.add(containerPort);

    // spec.container.volumeMounts信息
    var volumeMounts = new ArrayList<VolumeMount>();

    // 数据目录的挂载
    var storageVolumeMount = new VolumeMount();
    storageVolumeMount.setName(vastbaseSingleSpec.getVastbasePersistentStorageMountName());
    storageVolumeMount.setMountPath(vastbaseSingleSpec.getVastbasePersistentStorageMountPath());

    /*// 配置文件的挂载
    var cnfVolumeMount = new VolumeMount();
    cnfVolumeMount.setName(vastbaseSingleSpec.getVastbaseCnfMountName());
    cnfVolumeMount.setMountPath(vastbaseSingleSpec.getVastbaseCnfMountPath());
    cnfVolumeMount.setSubPath(vastbaseSingleSpec.getVastbaseCnfMountSubPath());*/

    volumeMounts.add(storageVolumeMount);
    //volumeMounts.add(cnfVolumeMount);

    // spec.container信息
    var container = new Container();
    container.setName(vastbaseSingleSpec.getContainerName());
    container.setImage(vastbaseSingleSpec.getImage());
    container.setResources(resourceRequirements);
    container.setPorts(containerPorts);
    container.setVolumeMounts(volumeMounts);

    var containers = new ArrayList<Container>();
    containers.add(container);

    // spec.volumes信息
    // PVC的挂载
    var persistentVolumeClaimVolumeSource = new PersistentVolumeClaimVolumeSource();
    persistentVolumeClaimVolumeSource.setClaimName(vastbaseSingleSpec.getPvcName());
    var pvcVolume = new Volume();
    pvcVolume.setName(vastbaseSingleSpec.getVastbasePersistentStorageMountName());

    pvcVolume.setPersistentVolumeClaim(persistentVolumeClaimVolumeSource);

    // cnf文件的挂载
    var keyToPath = new KeyToPath();
    keyToPath.setKey("my.cnf");
    keyToPath.setPath(vastbaseSingleSpec.getVastbaseCnfMountSubPath());
    var keyToPaths = new ArrayList<KeyToPath>();
    keyToPaths.add(keyToPath);

    var configMapVolumeSource = new ConfigMapVolumeSource();
    //configMapVolumeSource.setName(vastbaseSingleSpec.getConfigMapName());
    configMapVolumeSource.setItems(keyToPaths);

    var mycnfVolume = new Volume();
    mycnfVolume.setConfigMap(configMapVolumeSource);
    mycnfVolume.setName(vastbaseSingleSpec.getVolumeConfigName());

    var volumes = new ArrayList<Volume>();
    //volumes.add(mycnfVolume);
    volumes.add(pvcVolume);

    // template.spec的配置
    var podSpec = new PodSpec();
    podSpec.setContainers(containers);
    podSpec.setVolumes(volumes);

    // template配置
    var podTemplate = new PodTemplateSpec();
    podTemplate.setMetadata(podMeta);
    podTemplate.setSpec(podSpec);

    // spec配置
    var spec = new StatefulSetSpec();
    spec.setReplicas(vastbaseSingleSpec.getReplicas());
    spec.setServiceName(vastbaseSingleSpec.getHeadlessServiceName());
    spec.setSelector(labelSelector);
    spec.setTemplate(podTemplate);

    var statefulSet = new StatefulSet();
    statefulSet.setApiVersion(ApiVersionEnum.APPS_V1.getApiVersion());
    statefulSet.setKind(ResourceKindEnum.STATEFUL_SET.getKind());
    statefulSet.setMetadata(objectMeta);
    statefulSet.setSpec(spec);

    return statefulSet;
  }

  private ConfigMap buildConfigMap(VastbaseSingle vastbaseReplica) {
    VastbaseSingleSpec vastbaseSingleSpec = vastbaseReplica.getSpec();

    Map<String, String> labels = new HashMap<>();
    //labels.put("app", vastbaseSingleSpec.getConfigMapName());
    var objectMeta = new ObjectMeta();
    //objectMeta.setName(vastbaseSingleSpec.getConfigMapName());
    objectMeta.setLabels(labels);

    //Map<String, String> configData = vastbaseSingleSpec.getConfigData();

    var configMap = new ConfigMap();
    configMap.setApiVersion(ApiVersionEnum.V1.getApiVersion());
    configMap.setMetadata(objectMeta);
    configMap.setKind(ResourceKindEnum.CONFIG_MAP.getKind());
    //configMap.setData(configData);

    return configMap;
  }

  private PersistentVolumeClaim buildPersistentVolumeClaim(VastbaseSingle vastbaseReplica) {
    var vastbaseSingleSpec = vastbaseReplica.getSpec();

    // resource定义部分
    var resourceRequirements = new ResourceRequirements();
    var quantity = new Quantity();
    quantity.setAmount(vastbaseSingleSpec.getPvcStorageSize());
    Map<String, Quantity> capacity = new HashMap<>();
    capacity.put("storage", quantity);
    resourceRequirements.setRequests(capacity);

    // accessMode部分
    List<String> accessModes = new ArrayList<>();
    accessModes.add(AccessModeEnum.RWO.getAccessMode());

    // spec部分
    var spec = new PersistentVolumeClaimSpec();
    spec.setStorageClassName(vastbaseSingleSpec.getStorageClassName());
    spec.setAccessModes(accessModes);
    spec.setResources(resourceRequirements);

    // meta部分
    var objectMeta = new ObjectMeta();
    objectMeta.setName(vastbaseSingleSpec.getPvcName());

    var persistentVolumeClaim = new PersistentVolumeClaim();
    persistentVolumeClaim.setApiVersion(ApiVersionEnum.V1.getApiVersion());
    persistentVolumeClaim.setKind(ResourceKindEnum.PVC.getKind());
    persistentVolumeClaim.setMetadata(objectMeta);
    persistentVolumeClaim.setSpec(spec);

    return persistentVolumeClaim;
  }

  private PersistentVolume buildPersistentVolume(VastbaseSingle replica) {
    var vastbaseSingle = replica.getSpec();

    // 容量信息
    var quantity = new Quantity();
    quantity.setAmount(vastbaseSingle.getPvcStorageSize());
    Map<String, Quantity> capacity = new HashMap<>();
    capacity.put("storage", quantity);

    // 访问方式
    List<String> accessModes = new ArrayList<>();
    accessModes.add(AccessModeEnum.RWO.getAccessMode());

    // 路径信息
    var hostPathVolumeSource = new HostPathVolumeSource();
    hostPathVolumeSource.setPath(vastbaseSingle.getHostPath().get("path"));

    // PV的spec信息
    var spec = new PersistentVolumeSpec();
    spec.setStorageClassName(vastbaseSingle.getStorageClassName());
    spec.setHostPath(hostPathVolumeSource);
    spec.setAccessModes(accessModes);
    spec.setCapacity(capacity);

    var objectMeta = new ObjectMeta();
    objectMeta.setName(vastbaseSingle.getPvName());
    // PV声明头
    var persistentVolume = new PersistentVolume();
    persistentVolume.setApiVersion(ApiVersionEnum.V1.getApiVersion());
    persistentVolume.setKind(ResourceKindEnum.PV.getKind());
    persistentVolume.setSpec(spec);
    persistentVolume.setMetadata(objectMeta);

    return persistentVolume;
  }
}

