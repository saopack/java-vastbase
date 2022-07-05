package com.vastdata.cluster;

import io.fabric8.kubernetes.api.model.VolumeMount;
import lombok.Data;

import java.util.List;
import java.util.Map;

@Data
public class VastbaseClusterSpec {

    // Add Spec information here
    /**
     * 每个StatefulSet多少实例
     */
    private Integer replicas;

    /**
     * 集群创建用户
     */
    private String serviceAccountName;

    /**
     * 响应前置步骤中的Secret创建
     */
    private String secretName;

    /**
     * vastbase镜像，采用这种格式 vastbase:5.7.37
     */
    private String image;

    private String containerName;

    private String initContainerName;

    /**
     * 容器的资源限制，一般限制memory和cpu
     * <p>下面是一个示例</p>
     * <pre>
     *     memory: "256Mi"
     *     cpu: "500m"
     * </pre>
     */
    private Map<String, String> resourceLimits;

    /**
     * 容器的端口。vastbase默认5432
     */
    private Integer containerPort;

    /**
     * 容器的挂载点
     * 在本例中是两种挂载点，分别是数据文件目录和配置文件目录
     * <pre>
     *     - name: vastbase-persistent-storage
     *       mountPath: /usr/local/vastbasedata
     *     - name: vastbasecnf
     *       mountPath: /etc/my.cnf
     *       subPath: etc/my.cnf
     * </pre>
     */
    private List<VolumeMount> volumeMounts;

    /**
     * 定义PVC的容量
     */
    private String pvcStorageSize;

    /**
     * 存储卷的访问模式，有三种，
     * <li>ReadWriteOnce: 可以被一个节点读写挂载</li>
     * <li>ReadOnlyMany: 可以被多个节点只读挂载</li>
     * <li>ReadWriteMany: 可以被多个节点读写挂载</li>
     */
    private String[] accessModes;

    private String storageClassName;

    /**
     * 要挂载的路径，这是挂载本地盘用的，作为演示就用本地盘了，不考虑nfs
     */
    private Map<String, String> hostPath;
    /**
     * pv的meta中的name属性
     */
    private String pvName;

    /**
     * pvc的meta中的name属性
     */
    private String pvcName;

    /*    *//**
     * configMap的meta中的name属性
     *//*
    private String configMapName;

    *//**
     * 将所有的配置都可以写在这里
     *//*
    private Map<String, String> configData;*/

    /**
     * vastbase无头服务的名称
     */
    private String headlessServiceName;

    /**
     * 用于定义StatefulSet时，声明容器挂载目录的名称
     * spec.template.spec.containers.volumeMounts
     */
    private String vastbasePersistentStorageMountName;

    /**
     * 用于定义StatefulSet时，声明容器挂载目录的路径
     * spec.template.spec.containers.volumeMounts
     */
    private String vastbasePersistentStorageMountPath;

    /**
     * 与vastbasePersistentStorageMountName不同的是，这是声明挂载配置文件的
     * spec.template.spec.containers.volumeMounts
     */
    private String vastbaseCnfMountName;

    /**
     * 用于定义StatefulSet时，声明容器挂载目录的路径，这里声明的是配置文件的
     */
    private String vastbaseCnfMountPath;

    /**
     * 用于定义StatefulSet时，声明容器挂载目录的路径，这里声明的是配置文件的
     */
    private String vastbaseCnfMountSubPath;

    /**
     * 用于StatefulSet的定义
     * spec.template.spec.volumes这个数组中定义的configMap部分的信息
     */
    private String volumeConfigName;

    /**
     * 定义StatefulSet的名称
     */
    private String statefulSetName;

    private String vastbaseServiceName;

    private String vastbaseReadServiceName;

    private String vastbaseWriteServiceName;

    private Integer vastbaseServiceReadNodePort;

    private Integer vastbaseServiceWriteNodePort;

    private String namespace;
    
    private String maintain;
}
