#!/bin/bash
#
# THIS FILE IS PART OF Vastbase cluster PRODUCT
#
# Copyright (c) 2021-2021 广州海量数据库技术有限公司
#

scope_name=$SCOPE_NAME
service_name=$SERVICE_NAME
curr_node_name=$HOSTNAME
replicas=$REPLICAS
curr_node_ip=$HOST_IP
db_data_dir=/home/vastbase/vbdata
lic_path=/home/vastbase/vbbase/lic/.license

listen_port=55434
heartbeat_port=55435
service_port=55436


if [ -z $replicas ]
then
  replicas=1
fi
if [ -z $curr_node_name ]
then
  curr_node_name=vastbase
fi
if [ -z $curr_node_ip ]
then
  curr_node_ip=$(hostname -I|awk '{print $1}')
fi
if [ -z $scope_name ]
then
  scope_name=vastbase
fi

hba_conf=${db_data_dir}/pg_hba.conf
postgresql_conf=${db_data_dir}/postgresql.conf

for i in $(seq 0 $((${replicas}-1)))
do
  if [ "${curr_node_name##*-}" != "${i}" ]
    then
      other_node_ip_list="${other_node_ip_list}${other_node_ip_list:+,}${scope_name}-${i}.${service_name}"
  fi
done
other_node_ip_list=(${other_node_ip_list//,/ })
index=1
for ip in ${other_node_ip_list[@]}
do
  if [ ! "$ip" = "null" ]
      then 
  echo "replconninfo$index = 'localhost=${curr_node_ip} localport=${listen_port} localheartbeatport=${heartbeat_port} localservice=${service_port} remotehost=$ip remoteport=${listen_port} remoteheartbeatport=${heartbeat_port} remoteservice=${service_port}'"  >> $postgresql_conf
  let index+=1
  fi
done

echo "host all all 0.0.0.0/0 md5" >> $hba_conf
echo "license_path = '$lic_path'" >> $postgresql_conf
echo "remote_read_mode = 'non_authentication'" >> $postgresql_conf
echo "listen_addresses = '0.0.0.0'" >> $postgresql_conf
echo "port = '5432'" >> $postgresql_conf
echo "pgxc_node_name = 'vdb'" >> $postgresql_conf
echo "replication_type = '1'" >> $postgresql_conf