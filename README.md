# vastbase-operator

这个工程的主要目的是创建vastbase G100的k8s集群,包括数据持久化,集群内部故障转移,节点扩缩容等功能特性

## 0. 准备工作

本工程采用jdk11+开发，使用GraalVM虚拟机，使用Quarkus 2.7.5Final。本工程完全在Windows下开发测试，因此这里所有的操作都是可以支持Windows系统的。

下载好GraalVM后将环境变量中的Path和JavaHome指向其安装路径即可。只要`java -version`能够打印下面的结果，就表示正确的配置了：

```text
openjdk version "11.0.14" 2022-01-18
OpenJDK Runtime Environment GraalVM CE 22.0.0.2 (build 11.0.14+9-jvmci-22.0-b05)
OpenJDK 64-Bit Server VM GraalVM CE 22.0.0.2 (build 11.0.14+9-jvmci-22.0-b05, mixed mode, sharing)
```

Quarkus可以用chocolatey来安装，版本不会低于2.7.5Final，也可以在官网上按照指导进行安装。

## 1. 单实例MySQL的实现

要安装单机的MySQL，需要这些资源做支持：

* PV & PVC : 实现数据的持久化
* configMap : 实现配置的持久化
* StatefulSet : 这是一种特殊的Pod，一般有状态的服务都要使用StatefulSet
* headless Service : 无头服务
* Service : 提供对外读写的能力

因此controller的代码中主要的逻辑就是去创建这些资源。我准备了一份示例文件，保存在resources/cluster/中。要测试，需要执行下面的命令：

测试时可以采用本地测试的方式，执行下面几个命令：

```bash
make docker-build docker-push
make install
kubectl apply -f kubernetes.yml
cd vastbase-operator/src/main/resource/cluster/leak
kubectl apply -f operator-service-account-rbac.yaml
kubectl apply -f vastbase-secret.yaml
kubectl apply -f vastbase-cluster.yaml
```

quarkus支持实时模式，代码修改之后可以不用重启服务，稍等片刻就能重新加载完成。
