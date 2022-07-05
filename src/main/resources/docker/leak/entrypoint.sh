#!/bin/bash
other_ips=$PEER_IPS
data_dir=/home/vastbase/vbdata
host_ip=$HOST_IP
curr_node_name=$HOSTNAME
is_cluster=$ISCLUSTER
if [ -z $host_ip ]
then
  host_ip=$(hostname -I|awk '{print $1}')
fi
if [[ ! "$curr_node_name" =~ ^vastbase.* ]]
then
  curr_node_name=vastbase-0
fi

if [ ! -f "${data_dir}/postgresql.conf" ]
  then
    rm -rf ${data_dir}/*
    vb_initdb -U vastbase -E UTF8 --locale en_US.utf8 -D /home/vastbase/vbdata -w 1qaz@WSX --nodename vbd >>/home/vastbase/logs/vbinstall.log 2>&1
    /bin/bash /home/vastbase/scripts/postgresql_config.sh
fi

if [ "${curr_node_name##*-}" = "0" ]
  then
    if [ ! "$is_cluster" = "false" ] 
    then          
      vb_ctl start -M primary >> /home/vastbase/logs/vbstart.log 2>&1
    else          
      vb_ctl start >> /home/vastbase/logs/vbstart.log 2>&1
    fi
  else
    if [ ! -f "${data_dir}/postgresql.conf.bak" ]
      then
        vb_ctl build -b full >> /home/vastbase/logs/vbstart.log 2>&1   
      else
        vb_ctl start -M standby >> /home/vastbase/logs/vbstart.log 2>&1
    fi    
fi
tail -f /dev/null
