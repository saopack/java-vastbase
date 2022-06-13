#!/bin/bash
other_ips=$PEER_IPS
data_dir=/home/vastbase/vbdata
host_ip=$HOST_IP
curr_node_name=$HOSTNAME
if [ -z $host_ip ]
then
  host_ip=$(hostname -I|awk '{print $1}')
fi
if [ -z $curr_node_name ]yhe
then
  curr_node_name=vastbase-0
fi

chown -R vastbase:vastbase /home/vastbase

source "/home/vastbase/.bashrc"

if [ "${curr_node_name##*-}" = "0" ]
then
  if [ ! -f "${data_dir}/postgresql.conf" ]
  then
    rm -rf ${data_dir}/*
    su - vastbase -c "vb_initdb -U vastbase -E UTF8 --locale en_US.utf8 -D /home/vastbase/vbdata -w 1qaz@WSX --nodename vbnode >>/tmp/vbinstall.log 2>&1"
  fi
  ./home/vastbase/etcd/etcd -listen-client-urls http://$host_ip:2379,http://127.0.0.1:2379 -advertise-client-urls http://$host_ip:2379  -data-dir /home/vastbase/etcd/data  >>/tmp/etcd.log 2>&1 &
fi
/bin/bash /home/vastbase/scripts/patroni_config.sh
su - vastbase -c "/home/vastbase/vbbase/has/has /home/vastbase/vbbase/has/conf/has_conf.yml >>/tmp/has.log 2>&1"
tail -f /dev/null
