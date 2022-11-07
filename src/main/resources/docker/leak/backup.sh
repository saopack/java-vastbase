#!/bin/bash
backupRoot="/home/vastbase/backup/"
start_time=$(date "+%Y%m%d%H%M%S")
backupPath=$backupRoot$start_time
mkdir -p $backupPath
chmod 700 $backupPath
backuplog="/home/vastbase/logs/backup$start_time.log"
    
vb_backup_status="prepare"
vb_basebackup -D $backupPath -h 127.0.0.1 -p ${PGPORT} -F tar -z -X fetch >$backuplog 2>&1 &
pid_basebackup_flag=$(ps aux |grep -i "vb_basebackup -D $backupPath -h 127.0.0.1" |grep -v grep |wc -l)

#判断进程是否在, 每5s探测一次,一天内(即86400s)备份如果还没结束,则判断为超时
if [ "$pid_basebackup_flag" -eq "1" ]; then
  i=0
  while [ "$i" -lt "86400" ]
  do
    pid_basebackup_flag=$(ps aux |grep -i "vb_basebackup -D $backupPath -h 127.0.0.1" |grep -v grep |wc -l)
    if [ "$pid_basebackup_flag" -eq "1" ];then
      vb_backup_status="running"
      echo "vb_basebackup is running for "$i"s"
    else
      vb_backup_status="finished"
      echo "vb_basebackup complete, total time cost is "$i"s"
      break
    fi
    sleep 5
    i=$((${i} + 5))
  done
  if [ "$vb_backup_status" == "finished" ]; then
    #如果进程结束, 则通过备份日志判断是否成功
    backup_finished_flag=$(tail -n 1 $backuplog |grep -i "base backup successfully" |wc -l)
  else
    vb_backup_status="timeout"
    pid_basebackup=$(ps aux |grep -i "vb_basebackup -D $backupPath -h 127.0.0.1" |grep -v grep |awk '{printf $2}')
    echo "Backup timeout after "$i"s, backup process is "$pid_basebackup", the process will be killed"
    kill -9 $pid_basebackup
  fi
else
  vb_backup_status="failed"
  echo "vb_basebackup execute failed, Please check output in "$backuplog
fi

if [ "$backup_finished_flag" -eq "1" ]; then
  vb_backup_status="success"
  echo "vb_basebackup is successful"
else
  vb_backup_status="failed"
  echo "vb_basebackup execute failed, Please check output in "$backuplog
fi

cd  $backupRoot
tar -cvf $start_time.tar $start_time
rm -rf $start_time
backup_size=$(du -sb $backupRoot$start_time.tar |awk '{print $1}')
end_time=$(date "+%Y%m%d%H%M%S")
echo -e "start_time:$start_time \nend_time:$end_time \nbackup_status:$vb_backup_status \nbackup_file:$backupRoot$start_time.tar \nbackup_size=$backup_size">>"$backupRoot"vb_basebackup.log
