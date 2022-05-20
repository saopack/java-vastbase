#!/bin/bash

kubectl delete po mysql-0
kubectl delete statefulset mysql

kubectl delete pvc mysql-pv-claim

kubectl delete configmap mycnf
kubectl delete -n default secret my-secret

kubectl delete -n default service mysql

kubectl delete MysqlReplica my-replica

kubectl delete crd mysqlreplicas.mysql.vastdata.com
kubectl delete crd mysqlusers.mysql.vastdata.com