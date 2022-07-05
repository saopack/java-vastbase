#!/bin/bash
other_ips=$PEER_IPS
data_dir=/home/vastbase/vbdata
host_ip=$HOST_IP
curr_node_name=$HOSTNAME
is_cluster=$ISCLUSTER
replicas=$REPLICAS
scope_name=$SCOPE_NAME
service_name=$SERVICE_NAME
if [ -z $host_ip ]
then
  host_ip=$(hostname -I|awk '{print $1}')
fi
if [ -z $curr_node_name ]
then
  curr_node_name=vastbase-0
fi

if [ "${curr_node_name##*-}" = "0" ]
then
  if [ ! -f "${data_dir}/postgresql.conf" ]
  then
    rm -rf ${data_dir}/*
    vb_initdb -U vastbase -E UTF8 --locale en_US.utf8 -D /home/vastbase/vbdata -w 1qaz@WSX --nodename vbd >>/home/vastbase/logs/vbinstall.log 2>&1
    if [ ! "$is_cluster" = "false" ] 
    then
    vb_ctl start
    exit 0
    fi
  fi
fi



#if [ $replicas -ge 3 ]
#then
#    for i in $(seq 0 2)
#    do
#      etcd_cluster="${etcd_cluster}${etcd_cluster:+,}etcd${i}=http://${scope_name}-${i}.${service_name}:2380"
#    done    
#    if [ "${curr_node_name##*-}" -le 2 ]
#    then
#      ./etcd/etcd -name etcd${curr_node_name##*-} -initial-advertise-peer-urls http://$host_ip:2380 -listen-peer-urls http://$host_ip:2380 -listen-client-urls http://$host_ip:2379,http://127.0.0.1:2379 -advertise-client-urls http://$host_ip:2379 -initial-cluster-token vastbase-etcd -initial-cluster ${etcd_cluster} -initial-cluster-state new -data-dir /home/vastbase/etcd/data >> /home/vastbase/logs/etcd.log 2>&1 &
#    fi
#else
 if [ "${curr_node_name##*-}" = "0" ]
 then 
   ./etcd/etcd -listen-client-urls http://$host_ip:2379,http://127.0.0.1:2379 -advertise-client-urls http://$host_ip:2379  -data-dir /home/vastbase/etcd/data  >>/home/vastbase/logs/etcd.log 2>&1 &
# fi
fi
/bin/bash /home/vastbase/scripts/patroni_config.sh
/home/vastbase/vbbase/has/has /home/vastbase/vbbase/has/conf/has_conf.yml >>/home/vastbase/logs/has.log 2>&1
