#!/bin/bash
# THIS FILE IS PART OF Vastbase cluster PRODUCT
#
# Copyright (c) 2021-2021 广州海量数据库技术有限公司
#
#patroni集群回调脚本
#
readonly cb_name=$1
readonly role=$2
readonly scope=$3

function usage() {
    echo "Usage: $0 <on_start|on_stop|on_role_change> <role> <scope>";
    exit 1;
}

function master(){
    echo "当前POD为master，更新POD标签role: master"
    echo -e '[{"op": "replace", "path": "/metadata/labels/role", "value": "master"}]' > /home/vastbase/vbbase/has/patch_lable.json
    curl --cacert ${CACERT} --header "Authorization: Bearer ${TOKEN}" -X GET ${APISERVER}/api/v1/namespaces/{namespace}/pods/${HOSTNAME} --data "$(cat /home/vastbase/vbbase/has/patch_lable.json)"  --request PATCH -H "Content-Type:application/json-patch+json"
}

function slave(){
    echo "当前POD为slave，更新POD标签role: slave"
    echo -e '[{"op": "replace", "path": "/metadata/labels/role", "value": "slave"}]' > /home/vastbase/vbbase/has/patch_lable.json
    curl --cacert ${CACERT} --header "Authorization: Bearer ${TOKEN}" -X GET ${APISERVER}/api/v1/namespaces/{namespace}/pods/${HOSTNAME} --data "$(cat /home/vastbase/vbbase/has/patch_lable.json)"  --request PATCH -H "Content-Type:application/json-patch+json"
}

echo "`date +%Y-%m-%d\ %H:%M:%S,%3N` WARNING: patroni callback $cb_name $role $scope"
export APISERVER=https://kubernetes.default.svc
export SERVICEACCOUNT=/var/run/secrets/kubernetes.io/serviceaccount
export TOKEN=$(cat ${SERVICEACCOUNT}/token)

export CACERT=${SERVICEACCOUNT}/ca.crt

case $cb_name in
    on_stop)
        slave
        ;;
    on_start)
        if [[ $role == 'master' ]]; then
            master
        fi
        ;;
    on_role_change)
        if [[ $role == 'master' ]]; then
            master
        elif [[ $role == 'slave' ]]||[[ $role == 'replica' ]]||[[ $role == 'logical' ]]; then
            slave
        fi
        ;; 
    *)
        usage
        ;;
esac
