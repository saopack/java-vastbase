#!/bin/bash
#
# THIS FILE IS PART OF Vastbase cluster PRODUCT
#
# Copyright (c) 2021-2021 广州海量数据库技术有限公司
#
#patroni配置文件生成脚本

#patroni_home patroni安装目录(解压目录)
#scope_name patroni 集群名称(例如vastbase)
#curr_node_name 当前patroni节点名称
#curr_node_ip 当前节点ip
#etcd_hosts   etcd集群列表 格式为 ip1:port1,ip2:port2,ip3:port3  注意ip跟port要对应etcd的设置 例如172.16.101.117:2379,172.16.101.118:2379,172.16.101.119:2379
#db_port      数据库的监听端口 注意集群的的该端口需要相同
#db_data_dir  数据库数据目录 例如/home/vastbase/data/vastbase
#db_home      数据库安装目录 例如/home/vastbase/local/vastbase
#other_node_ip_list 除当前节点外其它节点ip列表, 格式为 ip,ip

#vip 数据库集群对外虚拟ip  注意虚拟ip地址需与patroni服务器属于同一网段
#vipbrd 虚拟ip网关
#vipnetmask 虚拟ip 掩码
#vipnetmaskbit 虚拟ip 位
#vipdev 虚拟ip网卡名称

#is_leader 是否为主节点 1:是 0:否
#user      启动patroni的用户

#restapi_port restpai 端口
#listen_port  监听端口
#heartbeat_port 心跳端口
#service_port 服务端口
#
#max_connections 最大连接数
#shared_buffers 共享内存大小
#work_mem 工作内存
#log_path:日志路径
#set_auto_start:是否开机启动
#

patroni_home=/home/vastbase/vbbase/has
scope_name=$SCOPE_NAME
service_name=$SERVICE_NAME
curr_node_name=$HOSTNAME
replicas=$REPLICAS
curr_node_ip=$HOST_IP
etcd_hosts="${scope_name}-0.${service_name}"
db_port=5432
db_data_dir=/home/vastbase/vbdata
db_home=/home/vastbase/vbbase

user=vastbase

restapi_port=8008
listen_port=55434
heartbeat_port=55435
service_port=55436
max_memory=$(free -m|grep "Mem"|awk '{print $2}')
max_connections=500
shared_buffers=256MB
work_mem=4MB
log_path=/home/vastbase/vbdata/logs
set_auto_start=1
lic_path=/home/vastbase/vbbase/lic/.license
adminPass=1qaz@WSX

if [ -z $patroni_home ]
then
  echo 'patroni home is empty! exit...'
  exit
fi
if [ -z $replicas ]
then
  replicas=1
fi
if [ -z $curr_node_name ]
then
  curr_node_name=vastbase-0
fi
if [ -z $curr_node_ip ]
then
  curr_node_ip=$(hostname -I|awk '{print $1}')
fi
if [ -z $scope_name ]
then
  etcd_hosts=$curr_node_ip
fi
#set dir/file name
[[ $patroni_home == */ ]] && patroni_home=${patroni_home%/*}

[[ $db_home == */ ]] && db_home=${db_home%/*}

[[ $db_data_dir == */ ]] && db_data_dir=${db_data_dir%/*}

if [ ! -d $patroni_home ]
then
  mkdir $patroni_home
fi

patroni_conf_file=${patroni_home}/conf/has_conf.yml
patroni_callback_sh=${patroni_home}/patroni_callback.sh
hba_conf=${db_data_dir}/pg_hba.conf

#create config.yml
cp -f /home/vastbase/scripts/patroni_conf_template.yml $patroni_conf_file

sed -i "s/{scope_name}/$scope_name/g" $patroni_conf_file
sed -i "s/{curr_node_name}/$curr_node_name/g" $patroni_conf_file
sed -i "s/{curr_node_ip}/$curr_node_ip/g" $patroni_conf_file
sed -i "s/{etcd_hosts}/$etcd_hosts/g" $patroni_conf_file
sed -i "s!{patroni_callback_sh}!$patroni_callback_sh!g" $patroni_conf_file
sed -i "s/{db_port}/$db_port/g" $patroni_conf_file
sed -i "s!{db_data_dir}!$db_data_dir!g" $patroni_conf_file
sed -i "s!{db_home}!$db_home!g" $patroni_conf_file
sed -i "s/{restapi_port}/$restapi_port/g" $patroni_conf_file
#sed -i "s/{node_dcsname_list}/$node_dcsname_list/g" $patroni_conf_file
sed -i "s/{db_max_connections}/$max_connections/g" $patroni_conf_file
sed -i "s/{db_shared_buffers}/$shared_buffers/g" $patroni_conf_file
sed -i "s#{db_work_mem}#$work_mem#g" $patroni_conf_file
sed -i "s#{db_log_dir}#$log_path#g" $patroni_conf_file
sed -i "s#{db_lic_path}#$lic_path#g" $patroni_conf_file
sed -i "s#{adminPass}#$adminPass#g" $patroni_conf_file

#patroni_callback.sh
sed -i "s#{namespace}#$NAMESPACE#g" $patroni_callback_sh

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
  #55行后面循环添加
  if [ ! "$ip" = "null" ]
      then 
  echo "    replconninfo$index: 'localhost=${curr_node_ip} localport=${listen_port} localheartbeatport=${heartbeat_port} localservice=${service_port} remotehost=$ip remoteport=${listen_port} remoteheartbeatport=${heartbeat_port} remoteservice=${service_port}'"  >> $patroni_conf_file
  let index+=1
  fi
done

sed -i "52a ??- host all $user $curr_node_ip/32 trust" $patroni_conf_file
echo "host all $user $curr_node_ip/32 trust" >> $hba_conf
echo "host all all 0.0.0.0/0 md5" >> $hba_conf
sed -i "s/?/ /g" $patroni_conf_file
sed -i "s/\r//g" $patroni_conf_file

#创建集群归档目录
if [ ! -d  ${db_data_dir}/arch ]
then
        mkdir ${db_data_dir}/arch
        chown -R ${user}:${user} ${db_data_dir}/arch
fi


#清空备库数据目录
[[ -n $db_data_dir ]] && [[ -d $db_data_dir ]] && [[ "${curr_node_name##*-}" != "0" ]] && echo "remove slave data dir: $db_data_dir" && rm -rf $db_data_dir/*

chown -R $user:$user $patroni_home
