#!/bin/bash
syntax() {
      echo "bash $0  -backupFile /gaussdata/backup/20211227221002.tar  -dataDir /gaussdata/data/db1"
      exit 22
    }
    # 初始化输入的参数
    for inopt in $@
    do
      case $(echo $inopt | tr a-z A-Z) in
        -BACKUPFILE) CurOpt="-BACKUPFILE";continue;;
        -DATADIR) CurOpt="-DATADIR";continue;;
        -LOGLEVEL|-L) CurOpt="-LOGLEVEL";continue;;
        -HELP|-H) CurOpt="HELP";syntax;;
        -*) CurOpt="";continue;;
      esac

      case "${CurOpt}" in
        -BACKUPFILE) typeset backupFile="${inopt}";continue;;
        -DATADIR) typeset dataDir="${inopt}";continue;;
        -LOGLEVEL) typeset -i LogLevel=${inopt:-3};continue;;
      esac
    done
    # 默认参数
    dataDir="${dataDir:-/home/vastbase/vbdata/}"
    start_time=$(date "+%Y%m%d%H%M%S")
    
    # 日志输出函数
    f_PrintLog() {
      case $(echo "$1"|tr "a-z" "A-Z") in
        DEBUG) typeset _LogLevel=4;typeset _LogFlag="DEBUG";;
        INFO|INFO_) typeset _LogLevel=3;typeset _LogFlag="INFO_";;
        WARN|WARN_) typeset _LogLevel=2;typeset _LogFlag="WARN_";;
        ERROR|STOP|STOP_) typeset _LogLevel=1;typeset _LogFlag="ERROR";;
        SUCC|SUCC_|SUCCESS) typeset _LogLevel=0;typeset _LogFlag="SUCC_";;
      esac
      if [ ${_LogLevel:-3} -le ${LogLevel:-3} ]
      then
        typeset LogFile="${LogFile:-"/tmp/${PShellName:-${ShellName}}.log"}"
        touch "${LogFile}" 2>/dev/null
        if [ -w "${LogFile}" ]
        then
            echo "[${_LogFlag}]:$(date "+%Y%m%d.%H%M%S"):${UserName}@${HostName}:${2}"|tee -a "${LogFile}";
        else
            echo "[${_LogFlag}]:$(date "+%Y%m%d.%H%M%S"):${UserName}@${HostName}:${2}";
        fi
      fi
      # Here defined if scirpt encountered an error,then exit the script
      [ "${_LogFlag}" = "ERROR" ] && exit 55 || return 0;
    }
    chkCmdOK() {
      if [ "$?" -eq 0 ]; then
        f_PrintLog "$1" "$2"
      fi
    }
    chkCmdNO() {
      if [ "$?" -ne 0 ]; then
        f_PrintLog "$1" "$2"
      fi
    }
    # 处理绝对路径的表空间
    funcEditTbs() {
      #判断是否存在自定义表空间
      count_backup_tar=$(ls -rlt ${backup_base_path}/*tar* |wc -l)
      #如果tar包数量大于1,代表除base.tar还有自定义表空间tar包, 相反则代表没有自定义表空间,则退出函数
      if [ "${count_backup_tar}" -gt "1" ]; then
        # 解压表空间备份文件
        gzip -d ./*.tar.gz
        tablespaceArr=($(cat ${dataDir}/tablespace_map|tr ' ' ':'))
        f_PrintLog "INFO" "List of tablespaces:  ${tablespaceArr[@]} "
        for ((i = 0; i < ${#tablespaceArr[@]}; i++)); do
          getTbsOid=$(echo ${tablespaceArr[i]} | awk -F: '{print $1}' )
          getTbsPath=$(echo ${tablespaceArr[i]} | awk -F: '{print $2}' )
          str=pg_location
          res=$(echo ${getTbsPath}|grep "${str}")
          if [ "${res}" != ""  ]; then
            mkdir -p ${getTbsPath}
            gs_tar -F ${getTbsOid}.tar -D $getTbsPath

            else
            mv ${getTbsPath} ${getTbsPath}.bak.${start_time}
            mkdir -p ${getTbsPath}
            chmod 700 ${getTbsPath}
            gs_tar -F ${getTbsOid}.tar -D ${getTbsPath}
          fi
        done
      else
        f_PrintLog "INFO" "No custom tablespace exists and no action is required"
      fi
    }
    # 备份原数据目录
    if [ ! -f ${backupFile} ];then
      f_PrintLog "INFO" "Cannot find data file ${backupFile}, restore DB data failed, exit."
      exit 1
    fi
    mv $dataDir $dataDir$start_time
    # 创建数据目录,并附权(700),权限错误将不能启动数据库
    mkdir -p ${dataDir}
    chmod 700 ${dataDir}
    
    # 解压备份文件
    backup_path=${backupFile%/*}
    f_PrintLog "INFO" "The tar package is decompressed: tar -xf $backupFile -C ${backup_path}... "
    tar -xf ${backupFile} -C ${backup_path}
    chkCmdOK "SUCC" "Decompress is successful"
    
    # 解压base.tar文件至原数据目录
    backup_base_path=${backupFile%.*}
    cd ${backup_base_path}
    f_PrintLog "INFO" "The tar.gz package is decompressed: gzip -d base.tar.gz..."
    gzip -d base.tar.gz
    chkCmdOK "SUCC" "Decompress base.tar.gz is successful"
    f_PrintLog "INFO" "The base.tar package is decompressed: gs_tar -F base.tar -D ${dataDir}..."
    gs_tar -F base.tar -D ${dataDir}
    chkCmdOK "SUCC" "Decompress base.tar.gz is successful"
    
    # 处理自定义表空间
    funcEditTbs
    # 清理解压目录
    rm -rf ${backup_base_path}
    f_PrintLog "INFO" "Restore DB data complete." 
