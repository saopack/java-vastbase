#!/bin/bash
data_dir=/home/vastbase/vbdata
if [ ! -f "${data_dir}/postgresql.conf" ]
  then
    rm -rf ${data_dir}/*
    vb_initdb -U vastbase -E UTF8 --locale en_US.utf8 -D /home/vastbase/vbdata -w 1qaz@WSX --nodename vbd >>/home/vastbase/logs/vbinstall.log 2>&1
    /bin/bash /home/vastbase/scripts/postgresql_config.sh
fi
     
vb_ctl start -M standby>> /home/vastbase/logs/vbstart.log 2>&1 &
tail -f /dev/null
